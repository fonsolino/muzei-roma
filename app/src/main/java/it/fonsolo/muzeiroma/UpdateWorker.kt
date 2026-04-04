package it.fonsolo.muzeiroma

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import androidx.work.workDataOf
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(applicationContext)

    companion object {
        private const val TAG = "UpdateWorker"
        private const val AUTHORITY = "it.fonsolo.muzeiroma.provider"

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "muzei_roma_background_fetch",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnceNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setInputData(workDataOf("manual" to true))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "muzei_roma_manual_fetch",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private suspend fun addLog(message: String, level: String = "INFO") {
        db.logDao().insert(LogEntity(message = message, level = level))
        Log.d(TAG, "[$level] $message")
    }

    override suspend fun doWork(): Result {
        val dao = db.artworkDao()
        val isManual = inputData.getBoolean("manual", false)

        if (!isManual) {
            addLog("Avvio sincronizzazione automatica v${BuildConfig.VERSION_NAME}")
        }

        DatabaseInitializer.populateDatabase(applicationContext)

        val artworksToDownload = dao.getArtworksToDownload()

        if (isManual) {
            addLog("Download manuale: controllo ${artworksToDownload.size} immagini rimanenti")
        }

        if (artworksToDownload.isNotEmpty()) {
            var downloadedInSession = 0
            for (artwork in artworksToDownload) {
                delay(3000) // Delay anti-ban Wikimedia

                if (!artwork.imageUrl.lowercase().contains(Regex("\\.(jpg|jpeg|png|webp)")) &&
                    !artwork.imageUrl.contains("Special:FilePath")) {
                    continue
                }

                try {
                    val response = executeRequest(artwork.imageUrl)
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val contentType = resp.header("Content-Type")
                            val extension = when {
                                contentType?.contains("image/png") == true -> "png"
                                contentType?.contains("image/webp") == true -> "webp"
                                else -> "jpg"
                            }

                            val file = getInternalFile(artwork.code, extension)
                            resp.body?.byteStream()?.use { input ->
                                FileOutputStream(file).use { output ->
                                    input.copyTo(output)
                                }
                            }

                            if (file.exists() && file.length() > 1024) {
                                val updatedArtwork = artwork.copy(
                                    isDownloaded = true,
                                    localUri = file.absolutePath
                                )
                                dao.updateArtwork(updatedArtwork)
                                downloadedInSession++
                                addLog("Scaricata: ${artwork.title}")
                            }
                        } else if (resp.code == 429) {
                            addLog("Server saturo (429). Riprovo tra poco.", "WARN")
                            return Result.retry()
                        } else {
                            addLog("Errore server (${resp.code}) per ${artwork.title}", "ERROR")
                        }
                    }
                } catch (e: Exception) {
                    addLog("Errore rete per ${artwork.title}: ${e.message}", "ERROR")
                }
                if (downloadedInSession >= 5) break
            }
        }

        syncWithMuzei(dao)
        db.logDao().pruneLogs()
        return Result.success()
    }

    private fun executeRequest(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MuzeiRoma/${BuildConfig.VERSION_NAME} (it.fonsolo.muzeiroma; contact@aibofobia.net)")
            .build()
        return client.newCall(request).execute()
    }

    private fun getInternalFile(code: String, extension: String): File {
        val dir = File(applicationContext.filesDir, "artworks")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$code.$extension")
    }

    private fun toMuzeiArtwork(entity: ArtworkEntity): Artwork {
        val byline = if (entity.author.isBlank()) {
            entity.location
        } else {
            "${entity.author} (${entity.date})"
        }
        val attribution = "${entity.location} • ${entity.form} (${entity.type})"
        return Artwork(
            token = entity.code,
            title = entity.title,
            byline = byline,
            attribution = attribution,
            persistentUri = Uri.parse(entity.imageUrl),
            webUri = entity.wikiIt.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        )
    }

    private suspend fun syncWithMuzei(dao: ArtworkDao) {
        val muzeiClient = ProviderContract.getProviderClient(applicationContext, AUTHORITY)
        val mode = RotationSettings.getMode(applicationContext)
        val rotationsCount = RotationSettings.getRotationsCount(applicationContext)

        val todayGiorno = RotationSettings.getTodayGiorno()
        val dayArtwork = dao.getArtworkByDay(todayGiorno)
        
        if (dayArtwork == null) {
            muzeiClient.setArtwork(dao.getAllArtworks().map { toMuzeiArtwork(it) })
            return
        }

        when (mode) {
            RotationSettings.MODE_DAY_ONLY -> {
                muzeiClient.setArtwork(listOf(toMuzeiArtwork(dayArtwork)))
            }
            else -> {
                val randoms = dao.getRandomArtworksExcluding(dayArtwork.code, rotationsCount * 3)
                val dayMuzei = toMuzeiArtwork(dayArtwork)
                val playlist = mutableListOf<Artwork>()
                var idx = 0
                repeat(3) {
                    playlist.add(dayMuzei)
                    repeat(rotationsCount) {
                        if (idx < randoms.size) {
                            playlist.add(toMuzeiArtwork(randoms[idx++]))
                        }
                    }
                }
                // Usiamo setArtwork per forzare il refresh dei metadati in Muzei
                muzeiClient.setArtwork(playlist)
            }
        }
    }
}
