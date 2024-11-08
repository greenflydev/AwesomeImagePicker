package `in`.myinnos.awesomeimagepicker.models

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable

class Video : Media {

    var duration: Long = 0

    constructor()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeLong(duration)
        dest.writeString(uri.toString())
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dest.writeBoolean(isFavorite)
        }
    }

    private constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString()
        duration = parcel.readLong()
        uri = Uri.parse(parcel.readString())
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isFavorite = parcel.readBoolean()
        }
    }

    companion object CREATOR : Parcelable.Creator<Video> {
        override fun createFromParcel(parcel: Parcel): Video {
            return Video(parcel)
        }

        override fun newArray(size: Int): Array<Video?> {
            return arrayOfNulls(size)
        }
    }
}
