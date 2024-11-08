package `in`.myinnos.awesomeimagepicker.models

import android.net.Uri

class Album(var id: Long,
            var name: String?,
            var uri: Uri?, mediaList: List<Media>?) {

    var mediaList: List<Media> = mediaList ?: mutableListOf()

    var count: Int = 0
        get() {
            return mediaList.size
        }
}
