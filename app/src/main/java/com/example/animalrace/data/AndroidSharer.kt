package com.example.animalrace.data

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import com.example.animalrace.ui.result.Sharer
import java.io.File

/** 결과 카드 이미지를 안드로이드 공유 창으로 보내거나 사진(갤러리)에 저장한다 (4.1) */
class AndroidSharer(private val activity: Activity) : Sharer {
    override fun share(image: ImageBitmap, text: String) {
        val dir = File(activity.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "result.png")
        file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(send, "결과 카드 보내기"))
    }

    override fun save(image: ImageBitmap): Boolean {
        // 안드로이드 10 미만은 저장 권한이 필요해서, 공유 창의 저장 기능을 쓰도록 안내한다
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "animal-race-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnimalRace")
        }
        val resolver = activity.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return runCatching {
            resolver.openOutputStream(uri)!!.use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.getOrElse { resolver.delete(uri, null, null); false }
    }
}
