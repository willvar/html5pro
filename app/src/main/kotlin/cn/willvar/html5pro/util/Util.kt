package cn.willvar.html5pro.util

import android.content.Intent
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat.recreate
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewCompat
import com.google.android.material.snackbar.Snackbar
import cn.willvar.html5pro.BuildConfig
import cn.willvar.html5pro.R
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import kotlin.system.exitProcess

fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
  ContextHolder.getCurrentActivity()?.runOnUiThread {
    Toast.makeText(ContextHolder.application, msg, duration).show()
  }
}

fun showSnack(msg: String, duration: Int = Snackbar.LENGTH_SHORT, action: String? = null, actionListener: View.OnClickListener? = null) {
  val context = ContextHolder.getCurrentActivity()
  context?.runOnUiThread {
    val snack = Snackbar.make(context.findViewById(android.R.id.content), msg, duration)
    if (action != null) {
      snack.setAction(action) {
        performHapticOnWebView()
      }
    }
    if (actionListener != null) {
      snack.setAction(action ?: context.getText(R.string.snack_bar_dismiss_button_text), actionListener)
    }
    snack.show()
  }
}

fun setStatusBarTextBlack() {
  ContextHolder.getCurrentActivity()?.let {
    val window = it.window
    window.decorView.post {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }
  }
}

fun setStatusBarTextWhite() {
  ContextHolder.getCurrentActivity()?.let {
    val window = it.window
    window.decorView.post {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }
  }
}

fun getAppEnvironment(): String {
  val context = ContextHolder.getCurrentActivity()
  return JSONObject().apply {
    put("appName", context?.getString(R.string.app_name))
    put("appVersion", BuildConfig.VERSION_NAME)
    put("systemVersion", Build.VERSION.SDK_INT)
    put("systemWebViewVersion", context?.let {
      WebViewCompat.getCurrentWebViewPackage(it)?.versionName
    } ?: "0.0.0.0")
  }.toString()  // 返回 JSON 字符串
}

fun resetPrivacySetting() {
  SettingStorage.set("privacy", "dialog", "")
  restartApp()
}

fun restartApp() {
  val context = ContextHolder.getCurrentActivity()
  context?.runOnUiThread {
    runBlocking {
      recreate(context)
    }
  }
}

fun performHaptic(view: View) {
  view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

fun performHapticOnWebView() {
  ContextHolder.getCurrentActivity()?.let { performHaptic(it.findViewById(R.id.webView)) }
}

fun shareText(text: String) {
  val context = ContextHolder.getCurrentActivity()
  // 创建分享意图
  val shareIntent = Intent().apply {
    action = Intent.ACTION_SEND
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, text)
  }
  // 唤起分享界面
  context?.startActivity(Intent.createChooser(shareIntent, context.getText(R.string.app_name)))
}

fun saveBase64ImageToGallery(image: String, filename: String) {
  val context = ContextHolder.getCurrentActivity()
  context?.let { ImageSaver(it) }?.saveBase64ImageToGallery(image, filename)
}

fun backToHomeHandle() {
  ContextHolder.getCurrentActivity()?.moveTaskToBack(false)
}

fun exitAppHandle() {
  ContextHolder.getCurrentActivity()?.finishAndRemoveTask()
  exitProcess(0)
}