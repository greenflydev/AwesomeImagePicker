package `in`.myinnos.awesomeimagepicker.helpers

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.Image
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.MediaType
import `in`.myinnos.awesomeimagepicker.models.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class GalleryUtil {
    companion object {

        private val TAG = GalleryUtil::class.java.simpleName

        val albums = mutableListOf<Album>()

        private const val INDEX_MEDIA_ID = MediaStore.MediaColumns._ID
        private const val INDEX_MEDIA_URI = MediaStore.MediaColumns.DATA
        private const val INDEX_MEDIA_SIZE = MediaStore.MediaColumns.SIZE
        private const val INDEX_MEDIA_MIMETYPE = MediaStore.MediaColumns.MIME_TYPE
        private const val INDEX_DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED
        private const val INDEX_ALBUM_ID = MediaStore.MediaColumns.BUCKET_ID
        private const val INDEX_ALBUM_NAME = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        private const val INDEX_DURATION = MediaStore.MediaColumns.DURATION

        suspend fun loadMedia(context: Context, mediaType: MediaType): Boolean {
            return withContext(Dispatchers.IO) {
                var success = false
                albums.clear()
                try {
                    val time = System.currentTimeMillis()
                    val totalMediaList: List<Media> = when (mediaType) {
                        MediaType.IMAGES -> getAllMediaList(context, QueryMediaType.IMAGE)
                        MediaType.VIDEOS -> getAllMediaList(context, QueryMediaType.VIDEO)
                        MediaType.MIXED -> {
                            val imageMediaList = getAllMediaList(context, QueryMediaType.IMAGE)
                            val videoMediaList = getAllMediaList(context, QueryMediaType.VIDEO)
                            (imageMediaList + videoMediaList).sortedByDescending { it.dateAddedSecond }
                        }
                    }

                    broadcastMediaLoaded(context, totalMediaList.size, (System.currentTimeMillis() - time))

                    val albumList: List<Album> = totalMediaList
                        .groupBy { media: Media -> media.albumId }
                        // This would sort the albums alphabetically
//                        .toSortedMap { albumName1: String, albumName2: String ->
//                            albumName1.compareTo(albumName2, true)
//                        }
                        .map { entry -> getAlbum(entry) }
                        .toList()

                    // Add all media album to the top
                    val totalAlbum = totalMediaList.run {
                        val albumName = context.getString(R.string.all_media)
                        Album(ConstantsCustomGallery.ALL_MEDIA_ALBUM_ID,
                            albumName,
                            firstOrNull()?.uri ?: Uri.EMPTY,
                            this
                        )
                    }

                    albums.addAll(mutableListOf(totalAlbum).apply { addAll(albumList) })

                    success = true

                } catch (e: Exception) {
                    // Will return success = false
                    Log.e(TAG, "Error loading albums, e = " + e.message)
                }
                success
            }
        }

        private fun broadcastMediaLoaded(context: Context, mediaCount: Int, loadingTime: Long) {
            /*
             * This will broadcast out that media was loaded into memory.
             * Used for tracking in the calling application.
             */
            val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
            localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_MEDIA_LOADED, true)
            localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA_COUNT, mediaCount)
            localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_LOADING_TIME, loadingTime);
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
        }

        private fun getAllMediaList(context: Context, queryMediaType: QueryMediaType): List<Media> {
            val sortOrder = "$INDEX_DATE_ADDED DESC"

            val projection = mutableListOf(
                INDEX_MEDIA_ID,
                INDEX_MEDIA_URI,
                INDEX_MEDIA_SIZE,
                INDEX_MEDIA_MIMETYPE,
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
            val cursor = context.contentResolver.query(
                    queryMediaType.contentUri,
                    projection,
                    selection,
                    null,
                    sortOrder) ?: return emptyList()

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
                    val mediaSize = getLong(getColumnIndexOrThrow(INDEX_MEDIA_SIZE))
                    val mimeType = getString(getColumnIndexOrThrow(INDEX_MEDIA_MIMETYPE))
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
                    media.size = mediaSize
                    media.mimeType = mimeType
                    media.dateAddedSecond = datedAddedSecond

                    media
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading media item, e = ${e.message}")
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
