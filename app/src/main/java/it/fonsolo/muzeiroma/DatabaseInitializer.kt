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

                // Formato ninfa.csv (v2, con colonna GIORNO):
                // [0] CODICE  [1] GIORNO  [2] AUTORE  [3] TITOLO EN  [4] TITOLO IT
                // [5] DATA    [6] TECNICA  [7] UBICAZIONE  [8] FORMA  [9] TIPO
                // [10] PERIODO  [11] IMMAGINE  [12] WIKI_IT  [13] WIKI_EN  [14] WIKI_FR

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
