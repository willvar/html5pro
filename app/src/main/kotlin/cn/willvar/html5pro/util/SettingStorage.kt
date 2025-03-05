package cn.willvar.html5pro.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// Storage 单例对象
object SettingStorage {
  private lateinit var appContext: Context

  // 初始化方法，传递 Application Context
  fun init(context: Context) {
    appContext = context.applicationContext
  }

  // 获取 SharedPreferences 实例
  private fun getSharedPreferences(name: String): SharedPreferences {
    return appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
  }

  // 获取字符串值
  fun get(name: String, key: String): String {
    val sharedPreferences = getSharedPreferences(name)
    return sharedPreferences.getString(key, "") ?: ""
  }

  // 存储字符串值
  fun set(name: String, key: String, value: String) {
    val sharedPreferences = getSharedPreferences(name)
    sharedPreferences.edit {
      putString(key, value)
    }
  }
}