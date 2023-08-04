package in.myinnos.awesomeimagepicker.models;

import android.net.Uri;
import android.os.Build;
import android.os.Parcel;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class Video extends Media {

    private long duration;

    public Video() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeString(getName());
        dest.writeLong(getDuration());
        dest.writeLong(getSize());
        dest.writeString(getMimeType());
        dest.writeString(getUri().toString());
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dest.writeBoolean(isFavorite());
        }
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel source) {
            return new Video(source);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    private Video(Parcel in) {
        setId(in.readLong());
        setName(in.readString());
        setDuration(in.readLong());
        setSize(in.readLong());
        setMimeType(in.readString());
        setUri(Uri.parse(in.readString()));
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setFavorite(in.readBoolean());
        }
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
