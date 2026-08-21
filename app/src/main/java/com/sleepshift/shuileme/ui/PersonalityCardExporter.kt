package com.sleepshift.shuileme.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.sleepshift.shuileme.model.PersonalityCardGenerator
import java.io.File

/**
 * 人格卡片导出（SL-6.5）。
 * Compose UI → Bitmap → PNG（cacheDir）→ FileProvider → ACTION_SEND。
 * 禁止第三方 SDK / 网络上传 / 云端存储。
 */
object PersonalityCardExporter {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** 保存 PNG 到 cacheDir，返回共享 Uri；失败返回 null */
    fun exportToFile(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "sleep_personality_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
        } catch (e: Exception) {
            null
        }
    }

    /** ACTION_SEND：图片 + 固定文本 */
    fun shareIntent(context: Context, uri: Uri, title: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, PersonalityCardGenerator.shareText(title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
