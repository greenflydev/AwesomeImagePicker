package in.myinnos.awesomeimagepicker.models;

import android.net.Uri;

import java.util.List;

/**
 * Created by MyInnos on 03-11-2016.
 */

public class Album {
    private long id;
    private String name;
    private Uri uri;
    private List<Media> mediaList;
    private int count;

    public Album() { }

    public Album(long id, String name, Uri uri) {
        this.id = id;
        this.name = name;
        this.uri = uri;
    }

    public Album(long id, String name, Uri uri, List<Media> mediaList) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.mediaList = mediaList;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCount() {
        if (mediaList != null) {
            return mediaList.size();
        }
        return 0;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Media> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<Media> mediaList) {
        this.mediaList = mediaList;
    }
}
