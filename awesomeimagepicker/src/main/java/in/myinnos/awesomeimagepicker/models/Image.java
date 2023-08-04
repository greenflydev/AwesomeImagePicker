package in.myinnos.awesomeimagepicker.models;

import android.net.Uri;
import android.os.Build;
import android.os.Parcel;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class Image extends Media {

    public Image() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeString(getName());
        dest.writeString(getMimeType());
        dest.writeLong(getSize());
        dest.writeString(getUri().toString());
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dest.writeBoolean(isFavorite());
        }
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    private Image(Parcel in) {
        setId(in.readLong());
        setName(in.readString());
        setMimeType(in.readString());
        setSize(in.readLong());
        setUri(Uri.parse(in.readString()));
        /*
         * Android 11 and up can check if the media is marked as favorite
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setFavorite(in.readBoolean());
        }
    }
}
