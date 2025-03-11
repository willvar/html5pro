package cn.willvar.html5pro

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import cn.willvar.html5pro.util.ImageSaver
import cn.willvar.html5pro.util.SettingStorage
import cn.willvar.html5pro.util.UpdateManager
import cn.willvar.html5pro.util.showSnack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
  private lateinit var app: MainApplication
  private lateinit var rootView: View
  private lateinit var webView: WebView
  private lateinit var loadingScreen: View
  private lateinit var loadingProgress: CircularProgressIndicator
  private lateinit var defaultUrl: String
  private lateinit var browseUrl: String
  private lateinit var updateManager: UpdateManager
  private lateinit var windowInsetsController: WindowInsetsControllerCompat
  private var statusBarHeight: Int = 0
  private var navigationBarHeight: Int = 0
  private var hasPageLoadedOnce = false
  private var webViewTimeOutJob: Job? = null
  private var debounceScrollJob: Job? = null
  private val remoteUpdateUrl = "https://www.example.com/app_update"
  private val userAgreementUrl = "https://www.example.com/user_agreement"
  private val userPrivacyUrl = "https://www.example.com/user_privacy"
  private var fileChoosePathCallback: ValueCallback<Array<Uri>>? = null
  private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
    fileChoosePathCallback?.run {
      onReceiveValue(uri?.let { arrayOf(it) })
      fileChoosePathCallback = null
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app = applicationContext as MainApplication
    // 初始化 datastore
    SettingStorage.init(this)
    // 半沉浸式边到边
    // https://developer.android.com/reference/androidx/activity/EdgeToEdge
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      ),
      navigationBarStyle = SystemBarStyle.auto(
        Color.TRANSPARENT,
        Color.TRANSPARENT
      )
    )
    // 消除半透明导航条背景
    if (Build.VERSION.SDK_INT >= 29) {
      window.isNavigationBarContrastEnforced = false
    }
    try {
      setContentView(R.layout.main_activity)
    } catch (e: Exception) {
      app.startErrorPage(getText(R.string.webview_package_broken).toString(), "0.0.0.0")
      return
    }
    rootView = findViewById(R.id.main_activity)
    webView = findViewById(R.id.webView)
    loadingScreen = findViewById(R.id.loadingScreen)
    loadingProgress = findViewById(R.id.loadingProgress)
    updateManager = UpdateManager(this, webView)
    windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    // webView 高度变化适应
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
      val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
      // 计算可用高度
      val availableHeight = rootView.height - imeHeight
      if (availableHeight > 0) {
        val params = webView.layoutParams
        params.height = availableHeight
        webView.layoutParams = params
      }
      // 返回消费后的 insets（最后一句省略了 return）
      WindowInsetsCompat.CONSUMED
    }
    // 根据平台给 url 赋值
    defaultUrl = when (val platform = getString(R.string.distribution_platform)) {
      in BuildConfig.PRODUCT_FLAVORS.split(",").toSet() -> {
        "https://$platform.example.com/"
      }

      "debug" -> {
        "file:///android_asset/test.html"
      }

      else -> {
        "https://www.example.com/"
      }
    }
    // 仅浏览：基础模式
    browseUrl = "$defaultUrl?basic=true"
    // 确保 rootView 已经初始化
    rootView.post {
      val insets = ViewCompat.getRootWindowInsets(rootView)
      if (insets != null) {
        statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
      }
      setupWebView()
      when (SettingStorage.get("privacy", "dialog")) {
        "agree" -> {
          loadingProgress.visibility = View.VISIBLE
          loadUrl(defaultUrl)
        }

        "disagree" -> {
          loadingProgress.visibility = View.VISIBLE
          loadUrl(browseUrl)
        }

        else -> {
          showPrivacyPolicyDialog()
        }
      }
    }
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        webView.evaluateJavascript("typeof qback === \"function\" ? qback() : history.back();", null)
      }
    })
  }

  // 用户授予权限后继续保存图片
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == ImageSaver.REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty() &&
      grantResults[0] == PackageManager.PERMISSION_GRANTED
    ) {
      ImageSaver.pendingBase64Image?.let { base64Str ->
        ImageSaver.pendingFilename?.let { filename ->
          ImageSaver(this).saveBase64ImageToGallery(base64Str, filename)
          ImageSaver.pendingBase64Image = null // 清空状态
          ImageSaver.pendingFilename = null
        }
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun setupWebView() {
    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    webView.clearCache(false)
    webView.clearFormData()
    webView.addJavascriptInterface(JsInterface(), "qc")
    webView.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      builtInZoomControls = false
      offscreenPreRaster = true
      mediaPlaybackRequiresUserGesture = false
      cacheMode = WebSettings.LOAD_NO_CACHE
      layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
      userAgentString = "${webView.settings.userAgentString} QuantumChrome/${BuildConfig.VERSION_NAME}"
    }
    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        Log.e("WebView", "shouldOverrideUrlLoading: $url")
        val domain = url.toHttpUrlOrNull()?.topPrivateDomain().toString()
        if (domain.contains("alipay.com")) {
          openWithAlipay(url)
          return true
        }
        if (url.startsWith("weixin://")) {
          openWithPackage(url, "com.tencent.mm", "微信")
          return true
        }
        if (url.startsWith("mqq://")) {
          openWithPackage(url, "com.tencent.mobileqq", "手机QQ")
          return true
        }
        // 返回 false 以只在 WebView 内加载
        return false
      }

      override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        if (detail?.didCrash() == true) {
          // 渲染进程崩溃
          Log.e("WebView", "WebView 渲染进程崩溃")
        } else {
          // 渲染进程被操作系统杀死，通常是由于内存不足
          Log.e("WebView", "WebView 渲染进程被系统杀死")
        }
        return true
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.e("WebView", "onPageFinished: $url")
        Log.e("WebView", "onPageFinished: ${webView.copyBackForwardList().currentIndex}")
        if (webView.copyBackForwardList().currentIndex != -1) {
          // 页面加载完成后隐藏 loadingScreen、显示 webView
          loadingScreen.visibility = View.GONE
          webView.visibility = View.VISIBLE
          webViewTimeOutJob?.cancel()
          if (!hasPageLoadedOnce) {
            // 首次加载完成触发
            lifecycleScope.launch {
              // 避免过早执行：首次加载完成时可能出错而触发 onReceivedError
              delay(1000)
              // try catch: 目标地址可能会拉取失败而导致 JSON 解析失败
              try {
                updateManager.checkForUpdate(JSONObject(updateManager.getLatestVersion(remoteUpdateUrl)))
              } catch (_: Exception) {
              }
            }
            webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
              handleScrollChange(scrollY, oldScrollY)
            }
            hasPageLoadedOnce = true
          }
          webView.evaluateJavascript(
            """
            (function() {
              document.documentElement.style.cssText = `
                --qc-status-bar-height: ${(statusBarHeight / resources.displayMetrics.density).toInt()}px;
                --qc-navigation-bar-height: ${(navigationBarHeight / resources.displayMetrics.density).toInt()}px;
              `;
            })();
            """.trimIndent(), null
          )
        }
      }

      override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
//        Log.e("WebView", "onReceivedError: ${error.description}")
        super.onReceivedError(view, request, error)
        webViewTimeOutJob?.cancel()
        // 检查是否为主框架请求，避免捕获 iframe 加载错误
        if (request.isForMainFrame) {
          loadingScreen.visibility = View.VISIBLE
          showErrorPage(error.description.toString())
        } else if (error.description.toString().contains("net::ERR_SSL_VERSION_OR_CIPHER_MISMATCH")) {
          // 客户端不支持服务端 SSL 版本
          loadingScreen.visibility = View.VISIBLE
          showErrorPage(getText(R.string.webview_error_ssl_version).toString())
        }
      }
    }
    webView.webChromeClient = object : WebChromeClient() {
      override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
//        consoleMessage?.message()?.let { Log.e("WebView", "onConsoleMessage: $it") }
        if (consoleMessage != null) {
          if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR && consoleMessage.message().contains("Uncaught SyntaxError")) {
            // 客户端不支持 webView 内的 JavaScript 语法
            showErrorPage(getText(R.string.webview_error_javascript_syntax).toString())
          }
        }
        return super.onConsoleMessage(consoleMessage)
      }

      override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
        fileChoosePathCallback?.onReceiveValue(null)
        fileChoosePathCallback = filePathCallback
        val acceptTypes = fileChooserParams.acceptTypes.filter { it.isNotEmpty() }.distinct()
        val mimeTypes = if (acceptTypes.isNotEmpty()) {
          acceptTypes.toTypedArray()
        } else {
          arrayOf("*/*")
        }
        filePickerLauncher.launch(mimeTypes)
        return true
      }
    }
  }

  // 由于 pageStart 视 webView 版本不同时机可能不准影响超时计时因此需要再次封装
  private fun loadUrl(url: String) {
    webViewTimeOutJob?.cancel()
    webViewTimeOutJob = watchWebViewTimeOut()
    webView.loadUrl(url)
    handleScrollChange(0, 0)
  }

  // 显隐状态栏
  // https://developer.android.com/develop/ui/views/layout/edge-to-edge#immersive-mode
  // https://developer.android.com/develop/ui/views/layout/immersive
  private fun handleScrollChange(scrollY: Int, oldScrollY: Int) {
    debounceScrollJob?.cancel() // 取消之前的任务
    debounceScrollJob = CoroutineScope(Dispatchers.Main).launch {
      delay(250)
      if (scrollY > oldScrollY) {
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
      } else {
        windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
      }
    }
  }

  private fun watchWebViewTimeOut(): Job {
    return CoroutineScope(Dispatchers.Main).launch {
      delay(10000)
      // 如果超时任务在规定时间内没有被取消，则执行超时处理
      if (isActive) {
        webView.stopLoading()
        showErrorPage(getText(R.string.webview_error_load_timeout).toString())
      }
    }
  }

  private fun showErrorPage(error: String) {
    webViewTimeOutJob?.cancel()
    app.startErrorPage(error, "${WebViewCompat.getCurrentWebViewPackage(this)?.versionName}")
  }

  private fun showPrivacyPolicyDialog() {
    MaterialAlertDialogBuilder(this).setCustomTitle(TextView(this).apply {
      text = getText(R.string.privacy_dialog_title)
      textSize = 20f
      setPadding(0, 60, 0, 60)
      gravity = Gravity.CENTER
      setTypeface(null, Typeface.BOLD)
    }).setView(TextView(this).apply {
      text = SpannableString(getText(R.string.privacy_dialog_content).toString().trimIndent()).apply {
        setClickableSpan(getText(R.string.privacy_agree_content_text).toString(), userAgreementUrl)
        setClickableSpan(getText(R.string.privacy_disagree_content_text).toString(), userPrivacyUrl)
      }
      movementMethod = LinkMovementMethod.getInstance()
      highlightColor = Color.TRANSPARENT
      setPadding(72, 0, 72, 0)
    }).setPositiveButton(getText(R.string.privacy_agree_button_text)) { _, _ ->
      SettingStorage.set("privacy", "dialog", "agree")
      loadingProgress.visibility = View.VISIBLE
      loadUrl(defaultUrl)
    }.setNegativeButton(getText(R.string.privacy_disagree_button_text)) { _, _ ->
      SettingStorage.set("privacy", "dialog", "disagree")
      loadingProgress.visibility = View.VISIBLE
      loadUrl(browseUrl)
    }.setCancelable(false).show()
  }

  private fun openWithAlipay(url: String) {
    Thread {
      try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connect()
        connection.inputStream
        try {
          val intent = Intent(Intent.ACTION_VIEW, connection.url.toString().toUri().getQueryParameter("scheme").toString().toUri())
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.setPackage("com.eg.android.AlipayGphone")
          startActivity(intent)
        } catch (e: ActivityNotFoundException) {
          showSnack("支付宝唤起失败，请在确认已安装后重试", Snackbar.LENGTH_INDEFINITE, getText(R.string.snack_bar_dismiss_button_text).toString())
        }
      } catch (e: Exception) {
        showSnack("支付宝唤起失败，请检查网络后重试", Snackbar.LENGTH_INDEFINITE, getText(R.string.snack_bar_dismiss_button_text).toString())
      }
    }.start()
  }

  private fun openWithPackage(url: String, packageName: String, appName: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      data = url.toUri()
      `package` = packageName
    }
    try {
      startActivity(intent)
    } catch (e: Exception) {
      showSnack("${appName}唤起失败，请在确认已安装后重试", Snackbar.LENGTH_INDEFINITE, getText(R.string.snack_bar_dismiss_button_text).toString())
    }
  }

  private fun SpannableString.setClickableSpan(clickableText: String, url: String) {
    val start = indexOf(clickableText)
    if (start >= 0) {
      setSpan(object : ClickableSpan() {
        private var isClickable = true  // 控制是否可以点击
        override fun onClick(widget: View) {
          if (isClickable) {
            isClickable = false  // 禁用后续点击
            PrivacyFragment.newInstance(url).show((widget.context as AppCompatActivity).supportFragmentManager, "${clickableText}Dialog")
            // 延迟重新启用点击事件
            widget.postDelayed({
              isClickable = true
            }, 500)
          }
        }

        override fun updateDrawState(ds: TextPaint) {
          ds.isUnderlineText = true
        }
      }, start, start + clickableText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
  }
}
