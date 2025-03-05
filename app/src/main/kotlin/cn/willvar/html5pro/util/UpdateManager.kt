package cn.willvar.html5pro.util

import android.content.Context
import android.content.Intent
import android.text.Html
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cn.willvar.html5pro.BuildConfig
import cn.willvar.html5pro.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context, private val rootView: WebView) {
  suspend fun getLatestVersion(urlString: String): String = withContext(Dispatchers.IO) {
    runCatching {
      val connection = URL(urlString).openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.inputStream.bufferedReader().use { it.readText() }
    }.getOrElse { "" }
  }

  fun checkForUpdate(jsonObject: JSONObject) {
    val versionCode = jsonObject.optJSONObject("data")?.optJSONObject("latest")?.optString("versionCode", "")
    if (versionCode != null && versionCode > BuildConfig.VERSION_CODE.toString()) {
      val change = jsonObject.optJSONObject("data")?.optString("change", "<p>修复了一些使用问题</p>")
      // 使用 Html.fromHtml 渲染 HTML 格式的内容
      MaterialAlertDialogBuilder(context)
        .setTitle("${context.getText(R.string.app_name)}")
        .setMessage(Html.fromHtml(change, Html.FROM_HTML_MODE_COMPACT))
        .setPositiveButton("立即更新") { dialog, _ ->
          prepareUpdate(jsonObject)
          dialog.dismiss()
        }
        .setNegativeButton("暂时不用") { dialog, _ ->
          dialog.dismiss()
        }
        .setCancelable(false).show()
    }
  }

  private fun prepareUpdate(jsonObject: JSONObject) {
    // 开始后台下载并安装
    (context as AppCompatActivity).lifecycleScope.launch {
      val fileName = "update.apk"
      if (jsonObject.optJSONObject("data")?.optString("download", "")?.let { downloadApk(it, fileName) } == true) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(
            FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              File(context.getCacheDir(), fileName)
            ),
            "application/vnd.android.package-archive"
          )
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
      }
    }
  }

  // 协程中下载文件
  private suspend fun downloadApk(downloadUrl: String, fileName: String): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        withContext(Dispatchers.Main) {
          Snackbar.make(rootView, "已在后台开始下载新版本", Snackbar.LENGTH_LONG).setAction("好的") {}.show()
        }
        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
        connection.connect()
        val input: InputStream = connection.inputStream
        val output = FileOutputStream(File(context.cacheDir, fileName))
        val data = ByteArray(4096)
        var count: Int
        while (input.read(data).also { count = it } != -1) {
          output.write(data, 0, count)
        }
        output.flush()
        output.close()
        input.close()
        true
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    }
  }
}