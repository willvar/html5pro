package cn.willvar.html5pro

import android.webkit.JavascriptInterface
import cn.willvar.html5pro.util.backToHomeHandle
import cn.willvar.html5pro.util.exitAppHandle
import cn.willvar.html5pro.util.getAppEnvironment
import cn.willvar.html5pro.util.performHapticOnWebView
import cn.willvar.html5pro.util.resetPrivacySetting
import cn.willvar.html5pro.util.saveBase64ImageToGallery
import cn.willvar.html5pro.util.setStatusBarTextBlack
import cn.willvar.html5pro.util.setStatusBarTextWhite
import cn.willvar.html5pro.util.shareText
import cn.willvar.html5pro.util.showToast

class JsInterface {
  @JavascriptInterface
  fun toast(content: String) {
    showToast(content)
  }

  @JavascriptInterface
  fun black() {
    setStatusBarTextBlack()
  }

  @JavascriptInterface
  fun white() {
    setStatusBarTextWhite()
  }

  @JavascriptInterface
  fun env(): String {
    return getAppEnvironment()
  }

  @JavascriptInterface
  fun haptic() {
    performHapticOnWebView()
  }

  @JavascriptInterface
  fun resetPrivacy() {
    resetPrivacySetting()
  }

  @JavascriptInterface
  fun saveImage(image: String, filename: String) {
    saveBase64ImageToGallery(image, filename)
  }

  @JavascriptInterface
  fun share(content: String) {
    performHapticOnWebView()
    shareText(content)
  }

  @JavascriptInterface
  fun backToHome() {
    backToHomeHandle()
  }

  @JavascriptInterface
  fun exitApp() {
    exitAppHandle()
  }
}