package cn.willvar.html5pro.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

object ContextHolder {
  lateinit var application: Application
  private lateinit var currentActivity: WeakReference<Activity>
  fun init(application: Application) {
    ContextHolder.application = application
    // 注册 Activity 生命周期回调
    application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = WeakReference(activity)
      }

      override fun onActivityStarted(activity: Activity) {}
      override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
      }

      override fun onActivityPaused(activity: Activity) {}
      override fun onActivityStopped(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {
        if (getCurrentActivity() == activity) {
          currentActivity.clear()
        }
      }
    })
  }

  fun getCurrentActivity(): Activity? {
    return currentActivity.get()
  }
}