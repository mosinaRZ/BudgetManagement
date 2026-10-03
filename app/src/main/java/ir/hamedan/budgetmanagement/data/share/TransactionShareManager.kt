package ir.hamedan.budgetmanagement.data.share

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Persists / shares the rendered transaction card.
 *
 * Saving: MediaStore on Android 10+ (no permission needed). On Android 8-9 the
 * legacy WRITE_EXTERNAL_STORAGE permission is required; see [needsLegacyStoragePermission].
 * Sharing: the PNG is written to cache/share (exposed through the existing FileProvider).
 */
object TransactionShareManager {

    private const val ALBUM = "Cidna"
    private const val SHARE_DIR = "share"
    private const val MAX_CACHE_AGE_MS = 24L * 60 * 60 * 1000

    /** True when saving to the gallery needs the runtime WRITE_EXTERNAL_STORAGE permission. */
    fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Saves the card to Pictures/Cidna. Returns the saved Uri, or null on failure. */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val fileName = newFileName()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null
                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
                    } ?: throw IOException("No output stream")
                    resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                    uri
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
                if (!dir.exists() && !dir.mkdirs()) return@withContext null
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
                }
                MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Opens the system share sheet with the card attached as a PNG. Returns false if it could not be shared. */
    suspend fun shareImage(context: Context, bitmap: Bitmap, chooserTitle: String): Boolean {
        val uri = withContext(Dispatchers.IO) { writeToShareCache(context, bitmap) } ?: return false
        return try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, chooserTitle)
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeToShareCache(context: Context, bitmap: Bitmap): Uri? = try {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        pruneOldFiles(dir)
        val file = File(dir, newFileName())
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        null
    }

    private fun pruneOldFiles(dir: File) {
        val cutoff = System.currentTimeMillis() - MAX_CACHE_AGE_MS
        dir.listFiles()?.forEach { if (it.isFile && it.lastModified() < cutoff) it.delete() }
    }

    private fun newFileName() = "Cidna_Transaction_${System.currentTimeMillis()}.png"
}