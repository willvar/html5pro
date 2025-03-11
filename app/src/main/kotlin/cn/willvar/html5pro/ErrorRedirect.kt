package cn.willvar.html5pro

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import cn.willvar.html5pro.util.performHaptic

class ErrorRedirect : AppCompatActivity() {
  private lateinit var errorTextView: TextView
  private lateinit var appInfoTextView: TextView
  private lateinit var retryButton: Button
  private lateinit var quitButton: Button
  private lateinit var app: MainApplication
  private lateinit var rootView: View

  @SuppressLint("SourceLockedOrientationActivity")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app = applicationContext as MainApplication
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    setContentView(R.layout.error_redirect)
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
    if (Build.VERSION.SDK_INT >= 29) {
      window.isNavigationBarContrastEnforced = false
    }
    rootView = findViewById(R.id.error_redirect)
    errorTextView = findViewById(R.id.error_message)
    appInfoTextView = findViewById(R.id.app_info)
    retryButton = findViewById(R.id.retry_button)
    quitButton = findViewById(R.id.quit_button)
    errorTextView.text = intent.getStringExtra("MESSAGE")
    appInfoTextView.text = intent.getStringExtra("APP_INFO")
    // 禁用长按事件即 return false
    rootView.setOnLongClickListener {
      false
    }
    // 设置按钮点击监听器
    retryButton.setOnClickListener {
      performHaptic(rootView)
      app.openUrlInBrowser("https://www.google.com")
    }
    quitButton.setOnClickListener {
      performHaptic(rootView)
      app.exitApp()
    }
  }
}