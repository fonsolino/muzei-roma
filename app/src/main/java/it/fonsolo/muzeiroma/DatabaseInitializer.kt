package it.fonsolo.muzeiroma

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

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
                // Usiamo Windows-1252 per risolvere i problemi di accenti tipici dei CSV Windows
                val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("Windows-1252")))
                
                reader.readLine() // Header
                
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.trim().isEmpty()) {
                        line = reader.readLine()
                        continue
                    }
                    val tokens = line.split(";")
                    if (tokens.size >= 11) {
                        val rawImageUrl = tokens[10].trim()
                        val cleanImageUrl = cleanWikiUrl(rawImageUrl)
                        
                        artworks.add(
                            ArtworkEntity(
                                code = tokens[0].trim(),
                                author = tokens[1].trim(),
                                titleEn = tokens[2].trim(),
                                title = tokens[3].trim(),
                                date = tokens[4].trim(),
                                technique = tokens[5].trim(),
                                location = tokens[6].trim(),
                                form = tokens[7].trim(),
                                type = tokens[8].trim(),
                                period = tokens[9].trim(),
                                imageUrl = cleanImageUrl,
                                wikiIt = if (tokens.size > 11) tokens[11].trim() else "",
                                wikiEn = if (tokens.size > 12) tokens[12].trim() else "",
                                wikiFr = if (tokens.size > 13) tokens[13].trim() else ""
                            )
                        )
                    }
                    line = reader.readLine()
                }
                reader.close()

                if (artworks.isNotEmpty()) {
                    dao.insertAll(artworks)
                    Log.d(TAG, "Database inizializzato con ${artworks.size} opere (Windows-1252).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore inizializzazione: ${e.message}")
            }
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
