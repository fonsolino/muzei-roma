package it.fonsolo.muzeiroma

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteActionCompat
import androidx.core.graphics.drawable.IconCompat
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
        private const val NETWORK_TIMEOUT = 30000
        private const val REQ_WIKI     = 10
        private const val REQ_SHARE    = 11
        private const val REQ_SETTINGS = 12
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
            val entity = db.artworkDao().getArtworkByCode(code)
                ?: throw FileNotFoundException("Opera non trovata")
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
                connection.setRequestProperty(
                    "User-Agent",
                    "MuzeiRoma/${BuildConfig.VERSION_NAME} (it.fonsolo.muzeiroma)"
                )
                connection.getInputStream()
            }
        }
    }

    override fun getCommandActions(artwork: Artwork): List<RemoteActionCompat> {
        val ctx = context ?: return emptyList()
        val actions = mutableListOf<RemoteActionCompat>()
        val icon = IconCompat.createWithResource(ctx, R.mipmap.ic_launcher)

        // Wikipedia
        artwork.webUri?.let { uri ->
            val pi = PendingIntent.getActivity(
                ctx, REQ_WIKI,
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            actions.add(RemoteActionCompat(icon, ctx.getString(R.string.command_wiki),
                ctx.getString(R.string.command_wiki), pi).apply { setShouldShowIcon(false) })
        }

        // Condividi
        val shareIntent = Intent(ctx, ShareActivity::class.java).apply {
            putExtra(ShareActivity.EXTRA_TOKEN,     artwork.token)
            putExtra(ShareActivity.EXTRA_TITLE,     artwork.title)
            putExtra(ShareActivity.EXTRA_BYLINE,    artwork.byline)
            putExtra(ShareActivity.EXTRA_WEB_URI,   artwork.webUri?.toString())
            putExtra(ShareActivity.EXTRA_IMAGE_URL, artwork.persistentUri?.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val sharePi = PendingIntent.getActivity(
            ctx, REQ_SHARE, shareIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(RemoteActionCompat(icon, ctx.getString(R.string.command_share),
            ctx.getString(R.string.command_share), sharePi).apply { setShouldShowIcon(false) })

        // Impostazioni
        val settingsPi = PendingIntent.getActivity(
            ctx, REQ_SETTINGS,
            Intent(ctx, MuzeiSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        actions.add(RemoteActionCompat(icon, ctx.getString(R.string.settings_title),
            ctx.getString(R.string.settings_title), settingsPi))

        return actions
    }
}
