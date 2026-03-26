package it.fonsolo.muzeiroma

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ShareActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOKEN     = "token"
        const val EXTRA_TITLE     = "title"
        const val EXTRA_BYLINE    = "byline"
        const val EXTRA_WEB_URI   = "web_uri"
        const val EXTRA_IMAGE_URL = "image_url"
        private const val TAG = "ShareActivity"
    }

    private var chooserLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token    = intent.getStringExtra(EXTRA_TOKEN)
        val title    = intent.getStringExtra(EXTRA_TITLE)  ?: ""
        val byline   = intent.getStringExtra(EXTRA_BYLINE) ?: ""
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)

        if (token == null) {
            Log.w(TAG, "token null, chiudo")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@ShareActivity)
                val entity = db.artworkDao().getArtworkByCode(token)
                val localFile = entity?.localUri?.let { File(it) }?.takeIf { it.exists() }

                val fileToShare = localFile ?: imageUrl?.let { url ->
                    withContext(Dispatchers.IO) { downloadImageToTemp(url) }
                }

                val shareText = buildShareText(title, byline)

                val shareIntent = if (fileToShare != null) {
                    val contentUri = FileProvider.getUriForFile(
                        this@ShareActivity, "${packageName}.fileprovider", fileToShare
                    )
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Log.d(TAG, "immagine non disponibile, condivido solo testo")
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                }

                startActivity(Intent.createChooser(shareIntent, getString(R.string.command_share)))
                chooserLaunched = true
            } catch (e: Exception) {
                Log.e(TAG, "Errore condivisione: ${e.message}", e)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (chooserLaunched) finish()
    }

    private fun downloadImageToTemp(imageUrl: String): File? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "MuzeiRoma/${BuildConfig.VERSION_NAME} (it.fonsolo.muzeiroma)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val ext = when {
                    response.header("Content-Type")?.contains("png")  == true -> "png"
                    response.header("Content-Type")?.contains("webp") == true -> "webp"
                    else -> "jpg"
                }
                val tempFile = File(cacheDir, "share_temp.$ext")
                response.body?.byteStream()?.use { inp ->
                    tempFile.outputStream().use { out -> inp.copyTo(out) }
                }
                if (tempFile.exists() && tempFile.length() > 1024) tempFile else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download immagine fallito: ${e.message}")
            null
        }
    }

    private fun buildShareText(title: String, byline: String): String {
        return buildString {
            append(title)
            if (byline.isNotEmpty()) {
                append("\n")
                append(byline)
            }
            append("\n\n")
            append("Inviato da MUZEI ROMA\n")
            append("Sito del progetto:\n")
            append(MainActivity.URL_SITE)
            append("\n\n")
            append("Repository:\n")
            append(MainActivity.URL_REPO)
        }
    }
}
