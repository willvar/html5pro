package cn.willvar.html5pro

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceRequest
import androidx.fragment.app.DialogFragment
import android.webkit.WebView
import android.webkit.WebViewClient

class PrivacyFragment : DialogFragment() {
  private lateinit var webView: WebView

  companion object {
    private const val ARG_URL = "url"
    fun newInstance(url: String): PrivacyFragment {
      val fragment = PrivacyFragment()
      val args = Bundle()
      args.putString(ARG_URL, url)
      fragment.arguments = args
      return fragment
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // 加载布局文件
    return inflater.inflate(R.layout.fragment_privacy, container, false)
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // 初始化 WebView
    webView = view.findViewById(R.id.webView)
    webView.settings.apply {
      javaScriptEnabled = true
    }
    webView.webViewClient = object : WebViewClient() {
      // 拦截所有跳转 URL
      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
      ): Boolean {
        return false
      }
    }
    // 获取传递的 URL 并加载
    val url = arguments?.getString(ARG_URL)
    url?.let {
      webView.loadUrl(it)
    }
  }

  // 在 onStart() 中调整 DialogFragment 的宽度
  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(resources.displayMetrics.widthPixels, WindowManager.LayoutParams.WRAP_CONTENT)
  }
}
