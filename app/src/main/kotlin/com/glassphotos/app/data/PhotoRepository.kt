package com.glassphotos.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A single photo pulled from the device's MediaStore. */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTakenMillis: Long,
    val widthPx: Int,
    val heightPx: Int,
    val sizeBytes: Long,
    val bucketName: String, // album / folder name
) {
    val aspectRatio: Float
        get() = if (heightPx == 0) 1f else widthPx.toFloat() / heightPx.toFloat()
}

/** A group of photos sharing a calendar day, newest first — mirrors the "Recents" day-sectioned layout. */
data class PhotoSection(
    val label: String,
    val photos: List<Photo>,
)

/** A device folder / album, e.g. "Camera", "Screenshots", "WhatsApp Images". */
data class Album(
    val name: String,
    val coverUri: Uri,
    val count: Int,
)

object PhotoRepository {

    suspend fun loadAllPhotos(context: Context): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateTaken = cursor.getLong(dateTakenCol).let {
                    if (it > 0) it else cursor.getLong(dateAddedCol) * 1000L
                }
                val uri = ContentUris.withAppendedId(collection, id)
                photos += Photo(
                    id = id,
                    uri = uri,
                    displayName = cursor.getString(nameCol) ?: "IMG_$id",
                    dateTakenMillis = dateTaken,
                    widthPx = cursor.getInt(widthCol),
                    heightPx = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    bucketName = cursor.getString(bucketCol) ?: "Unknown",
                )
            }
        }
        photos
    }

    /** Groups photos into day-sections, e.g. "Today", "Yesterday", "July 12, 2026". */
    fun groupByDay(photos: List<Photo>): List<PhotoSection> {
        if (photos.isEmpty()) return emptyList()

        val now = Date()
        val todayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = todayFmt.format(now)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = todayFmt.format(cal.time)

        val displayFmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        return photos
            .groupBy { dayKeyFmt.format(Date(it.dateTakenMillis)) }
            .toSortedMap(compareByDescending { it })
            .map { (key, group) ->
                val label = when (key) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> displayFmt.format(Date(group.first().dateTakenMillis))
                }
                PhotoSection(label = label, photos = group)
            }
    }

    /** Derives albums (device folders) from the flat photo list. */
    fun groupByAlbum(photos: List<Photo>): List<Album> {
        return photos
            .groupBy { it.bucketName }
            .map { (name, group) ->
                Album(
                    name = name,
                    coverUri = group.first().uri,
                    count = group.size,
                )
            }
            .sortedByDescending { it.count }
    }
}
