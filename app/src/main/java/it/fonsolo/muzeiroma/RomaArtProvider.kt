package it.fonsolo.muzeiroma

import android.util.Log
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

class RomaArtProvider : MuzeiArtProvider() {

    companion object {
        private const val TAG = "RomaArtProvider"
        private const val COMMAND_ID_OPEN_WIKIPEDIA = 2
        private const val NETWORK_TIMEOUT = 30000 
    }

    override fun onLoadRequested(initial: Boolean) {
        Log.d(TAG, "onLoadRequested(initial=$initial)")
        val context = checkNotNull(context)
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            if (db.artworkDao().getCount() == 0) {
                UpdateWorker.runOnceNow(context)
            }
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        val code = artwork.token ?: throw FileNotFoundException("Token mancante")
        val context = checkNotNull(context)
        val db = AppDatabase.getDatabase(context)
        
        return runBlocking {
            val entity = db.artworkDao().getArtworkByCode(code) ?: throw FileNotFoundException("Opera non trovata")
            val localFile = entity.localUri?.let { File(it) }

            if (localFile != null && localFile.exists()) {
                Log.d(TAG, "Apertura file locale (centrato): ${entity.title}")
                FileInputStream(localFile)
            } else {
                Log.d(TAG, "Apertura stream remoto per: ${entity.title}")
                val url = URL(entity.imageUrl)
                val connection: URLConnection = url.openConnection()
                connection.connectTimeout = NETWORK_TIMEOUT
                connection.readTimeout = NETWORK_TIMEOUT
                connection.setRequestProperty("User-Agent", "MuzeiRoma/${BuildConfig.VERSION_NAME} (it.fonsolo.muzeiroma)")
                connection.getInputStream()
            }
        }
    }

    override fun getCommands(artwork: Artwork): List<UserCommand> {
        return listOf(
            UserCommand(COMMAND_ID_OPEN_WIKIPEDIA, "Dettagli su Wikipedia")
        )
    }

    override fun onCommand(artwork: Artwork, id: Int) {
        if (id == COMMAND_ID_OPEN_WIKIPEDIA) {
            artwork.webUri?.let { uri ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context?.startActivity(intent)
            }
        }
    }
}
