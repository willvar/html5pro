package cn.willvar.html5pro

import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import cn.willvar.html5pro.util.ContextHolder
import cn.willvar.html5pro.util.showToast
import androidx.core.net.toUri

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    ContextHolder.init(application = this)
  }

  fun openUrlInBrowser(url: String) {
    val context = ContextHolder.getCurrentActivity()
    if (context != null) {
      showToast(context.getText(R.string.open_with_default_browser).toString())
      val intent = Intent(Intent.ACTION_VIEW).apply {
        data = url.toUri()
      }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
      }
    }
  }

  fun startErrorPage(error: String, wvVersion: String) {
    val intent = Intent(this, ErrorRedirect::class.java)
    intent.putExtra("MESSAGE", error)
    intent.putExtra("APP_INFO", "${BuildConfig.VERSION_NAME}-${Build.VERSION.SDK_INT}-${wvVersion}")
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
    startActivity(
      intent, ActivityOptions.makeCustomAnimation(
        this,
        android.R.anim.fade_in,
        android.R.anim.fade_out
      ).toBundle()
    )
    ContextHolder.getCurrentActivity()?.finish()
  }

  fun exitApp() {
    ContextHolder.getCurrentActivity()?.finishAndRemoveTask()
  }
}