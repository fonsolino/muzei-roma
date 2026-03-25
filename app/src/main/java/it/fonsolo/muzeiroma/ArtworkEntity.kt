package it.fonsolo.muzeiroma

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artworks")
data class ArtworkEntity(
    @PrimaryKey val code: String,
    val day: Int = 0,
    val author: String,
    val title: String,
    val titleEn: String,
    val date: String,
    val technique: String,
    val location: String,
    val form: String,
    val type: String,
    val period: String,
    val imageUrl: String,
    val wikiIt: String,
    val wikiEn: String,
    val wikiFr: String,
    val isDownloaded: Boolean = false,
    val localUri: String? = null
)
