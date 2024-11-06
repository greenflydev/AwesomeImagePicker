package `in`.myinnos.awesomeimagepicker.helpers

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.Image
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.MediaStoreType
import `in`.myinnos.awesomeimagepicker.models.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal class GalleryUtil {
    companion object {

        private const val INDEX_MEDIA_ID = MediaStore.MediaColumns._ID
        private const val INDEX_MEDIA_URI = MediaStore.MediaColumns.DATA
        private const val INDEX_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED
        private const val INDEX_ALBUM_ID = MediaStore.MediaColumns.BUCKET_ID
        private const val INDEX_ALBUM_NAME = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        private const val INDEX_DURATION = MediaStore.MediaColumns.DURATION

        fun getMediaFromJava(context: Context, mediaType: MediaStoreType, callback: (List<Album>) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val albums = getMedia(context, mediaType)
                withContext(Dispatchers.Main) {
                    callback(albums)
                }
            }
        }

        suspend fun getMedia(context: Context, mediaStoreType: MediaStoreType): List<Album> {
            return withContext(Dispatchers.IO) {
                var result = mutableListOf<Album>()
                try {
                    val totalMediaList: List<Media> = when (mediaStoreType) {
                        MediaStoreType.IMAGES -> getAllMediaList(context, QueryMediaType.IMAGE)
                        MediaStoreType.VIDEOS -> getAllMediaList(context, QueryMediaType.VIDEO)
                        MediaStoreType.MIXED -> {
                            val imageMediaList = getAllMediaList(context, QueryMediaType.IMAGE)
                            val videoMediaList = getAllMediaList(context, QueryMediaType.VIDEO)
                            (imageMediaList + videoMediaList).sortedByDescending { it.dateAddedSecond }
                        }
                    }
                    val albumList: List<Album> = totalMediaList
                        .groupBy { media: Media -> media.albumId }
//                        .toSortedMap { albumName1: String, albumName2: String ->
//                            albumName1.compareTo(albumName2, true)
//                        }
                        .map { entry -> getAlbum(entry) }
                        .toList()


                    val totalAlbum = totalMediaList.run {
                        val albumName = "All" //context.getString(R.string.ted_image_picker_album_all)
                        Album(0,
                            albumName,
                            firstOrNull()?.uri ?: Uri.EMPTY,
                            this
                        )
                    }

                    result = mutableListOf(totalAlbum).apply { addAll(albumList) }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                result
            }
        }

        private fun getAllMediaList(context: Context, queryMediaType: QueryMediaType): List<Media> {
            val sortOrder = "$INDEX_DATE_ADDED DESC"

            val projection = mutableListOf(
                INDEX_MEDIA_ID,
                INDEX_MEDIA_URI,
                INDEX_ALBUM_ID,
                INDEX_ALBUM_NAME,
                INDEX_DATE_ADDED,
            ).apply {
                if (queryMediaType == QueryMediaType.VIDEO) {
                    add(INDEX_DURATION)
                }
            }.toTypedArray()
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.SIZE + " > 0"
            } else {
                null
            }
            val cursor =
                context.contentResolver.query(
                    queryMediaType.contentUri,
                    projection,
                    selection,
                    null,
                    sortOrder
                )
                    ?: return emptyList()

            cursor.use {
                return generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { getMedia(it, queryMediaType) }
                    .filterNotNull()
                    .toList()
            }
        }

        private fun getAlbum(entry: Map.Entry<Long, List<Media>>) =
            Album(entry.key, entry.value[0].albumName, entry.value[0].uri, entry.value)

        private fun getMedia(cursor: Cursor, queryMediaType: QueryMediaType): Media? =
            try {
                cursor.run {
                    val albumId = getLong(getColumnIndexOrThrow(INDEX_ALBUM_ID))
                    val albumName = getString(getColumnIndexOrThrow(INDEX_ALBUM_NAME))
                    val mediaId = getLong(getColumnIndexOrThrow(INDEX_MEDIA_ID))
                    val mediaUri = getMediaUri(queryMediaType)
                    val datedAddedSecond = getLong(getColumnIndexOrThrow(INDEX_DATE_ADDED))

                    val media = when (queryMediaType) {
                        QueryMediaType.IMAGE -> Image()
                        QueryMediaType.VIDEO -> Video().apply {
                            duration = getLong(getColumnIndexOrThrow(INDEX_DURATION))
                        }
                    }

                    media.id = mediaId
                    media.albumId = albumId
                    media.albumName = albumName
                    media.uri = mediaUri
                    media.dateAddedSecond = datedAddedSecond

                    media
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }

        private fun Cursor.getMediaUri(queryMediaType: QueryMediaType): Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val id = getLong(getColumnIndexOrThrow(INDEX_MEDIA_ID))
                ContentUris.withAppendedId(queryMediaType.contentUri, id)
            } else {
                val mediaPath = getString(getColumnIndexOrThrow(INDEX_MEDIA_URI))
                Uri.fromFile(File(mediaPath))
            }

        private enum class QueryMediaType(val contentUri: Uri) {
            IMAGE(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            VIDEO(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
    }
}
