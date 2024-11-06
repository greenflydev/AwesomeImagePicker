package in.myinnos.awesomeimagepicker.models;

import android.net.Uri;
import android.os.Parcelable;

/**
 * Created by pcm2a on 10-04-2019
 */
public abstract class Media implements Parcelable {

    private long id;
    private String name;
    private long size;
    private String mimeType;
    private Uri uri;
    private boolean isSelected;
    private boolean isFavorite;
    private long albumId;
    private String albumName;
    private long dateAddedSecond;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public long getDateAddedSecond() {
        return dateAddedSecond;
    }

    public void setDateAddedSecond(long dateAddedSecond) {
        this.dateAddedSecond = dateAddedSecond;
    }
}
