package it.fonsolo.muzeiroma

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object DatabaseInitializer {
    private const val TAG = "DatabaseInitializer"

    suspend fun populateDatabase(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.artworkDao()

        if (dao.getCount() > 0) return

        withContext(Dispatchers.IO) {
            try {
                val artworks = mutableListOf<ArtworkEntity>()
                val inputStream = context.assets.open("ninfa.csv")
                // Utilizziamo UTF-8 standard per interpretare correttamente il file
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))

                reader.readLine() // Header

                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.trim().isEmpty()) {
                        line = reader.readLine()
                        continue
                    }
                    val tokens = line.split(";")
                    if (tokens.size >= 12) {
                        val rawImageUrl = tokens[11].trim()
                        val cleanImageUrl = cleanWikiUrl(rawImageUrl)

                        artworks.add(
                            ArtworkEntity(
                                code = tokens[0].trim(),
                                day = tokens[1].trim().toIntOrNull() ?: 0,
                                author = tokens[2].trim(),
                                titleEn = tokens[3].trim(),
                                title = tokens[4].trim(),
                                date = tokens[5].trim(),
                                technique = tokens[6].trim(),
                                location = tokens[7].trim(),
                                form = tokens[8].trim(),
                                type = tokens[9].trim(),
                                period = tokens[10].trim(),
                                imageUrl = cleanImageUrl,
                                wikiIt = if (tokens.size > 12) tokens[12].trim() else "",
                                wikiEn = if (tokens.size > 13) tokens[13].trim() else "",
                                wikiFr = if (tokens.size > 14) tokens[14].trim() else ""
                            )
                        )
                    }
                    line = reader.readLine()
                }
                reader.close()

                if (artworks.isNotEmpty()) {
                    dao.insertAll(artworks)
                    Log.d(TAG, "Database inizializzato con ${artworks.size} opere (UTF-8).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore inizializzazione CSV: ${e.message}")
            }
        }
    }

    suspend fun migrateUrls(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.artworkDao()
        val lupa = dao.getArtworkByCode("E0008-MSA")
        if (lupa != null && !lupa.imageUrl.contains("Special:FilePath")) {
            dao.updateArtwork(lupa.copy(
                imageUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/Lupa_Capitolina_con_sfondo_bianco.jpg",
                isDownloaded = false,
                localUri = null
            ))
        }
    }

    private fun cleanWikiUrl(url: String): String {
        if (url.contains("/media/File:")) {
            val fileName = url.substringAfter("/media/File:")
            return "https://commons.wikimedia.org/wiki/Special:FilePath/$fileName"
        }
        return url
    }
}
