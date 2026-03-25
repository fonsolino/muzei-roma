package it.fonsolo.muzeiroma

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Query("SELECT * FROM artworks ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomArtwork(): ArtworkEntity?

    @Query("SELECT * FROM artworks WHERE isDownloaded = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomDownloadedArtwork(): ArtworkEntity?

    @Query("SELECT * FROM artworks")
    suspend fun getAllArtworks(): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE isDownloaded = 0")
    suspend fun getArtworksToDownload(): List<ArtworkEntity>

    @Query("SELECT COUNT(*) FROM artworks WHERE isDownloaded = 1")
    fun getDownloadedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM artworks")
    fun getAllCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(artworks: List<ArtworkEntity>)

    @Query("SELECT COUNT(*) FROM artworks")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM artworks WHERE isDownloaded = 1")
    suspend fun getDownloadedCount(): Int

    @Update
    suspend fun updateArtwork(artwork: ArtworkEntity)

    @Query("SELECT * FROM artworks WHERE code = :code")
    suspend fun getArtworkByCode(code: String): ArtworkEntity?

    @Query("SELECT * FROM artworks WHERE day = :day LIMIT 1")
    suspend fun getArtworkByDay(day: Int): ArtworkEntity?

    @Query("SELECT * FROM artworks WHERE code != :excludeCode ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomArtworksExcluding(excludeCode: String, limit: Int): List<ArtworkEntity>
}

@Database(entities = [ArtworkEntity::class, LogEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun artworkDao(): ArtworkDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nessun cambiamento allo schema
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "muzei_roma_db"
                )
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
