package `in`.myinnos.awesomeimagepicker.models

import android.net.Uri
import android.os.Parcelable

/**
 * Created by pcm2a on 10-04-2019
 */
abstract class Media : Parcelable {

    var id: Long = 0
    var name: String? = null
    var uri: Uri? = null
    var albumId: Long = 0
    var albumName: String? = null
    var dateAddedSecond: Long = 0

    /*
     * These don't come from the media content provider
     */
    var isSelected: Boolean = false
    var isFavorite: Boolean = false
}

/*
internal sealed class Media(
    open val albumName: String,
    open val uri: Uri,
    open val dateAddedSecond: Long,
) {
    data class Image(
        override val albumName: String,
        override val uri: Uri,
        override val dateAddedSecond: Long,
    ) : Media(albumName, uri, dateAddedSecond)
 */
