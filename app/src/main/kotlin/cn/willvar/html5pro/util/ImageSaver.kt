package cn.willvar.html5pro.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStream

class ImageSaver(private val context: Context) {
  companion object {
    var pendingBase64Image: String? = null
    var pendingFilename: String? = null
    const val REQUEST_CODE_STORAGE_PERMISSION = 1001
  }

  // 主函数：保存 Base64 图片到图库
  fun saveBase64ImageToGallery(base64Str: String, filename: String) {
    val activity = ContextHolder.getCurrentActivity() ?: return // 获取 Activity
    val bitmap = base64ToBitmap(base64Str) ?: return // Base64 转 Bitmap
    if (Build.VERSION.SDK_INT > 28) {
      // Android 10 及以上，直接保存图片
      saveImageToGallery(context, bitmap, filename)
      showToast("图片已保存至您的图库")
    } else {
      // Android 10 以下，先检查权限
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
      ) {
        // 请求权限
        ActivityCompat.requestPermissions(
          activity,
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          REQUEST_CODE_STORAGE_PERMISSION
        )
        // 保存全局状态，等待权限回调
        pendingBase64Image = base64Str
        pendingFilename = filename
      } else {
        // 有权限直接保存
        saveImageToGallery(context, bitmap, filename)
        showToast("图片已保存至您的图库")
      }
    }
  }

  // 保存 Bitmap 到图库
  private fun saveImageToGallery(context: Context, bitmap: Bitmap, filename: String) {
    val contentResolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
      put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
      put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
      put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
      }
    }
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
      val outputStream: OutputStream? = contentResolver.openOutputStream(it)
      outputStream?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // 将图片保存
      }
    }
  }

  // Base64 转 Bitmap
  private fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
      val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
