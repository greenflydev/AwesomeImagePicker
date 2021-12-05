package in.myinnos.awesomeimagepicker.activities;

import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_in;
import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_out;

import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.adapter.AlbumSelectAdapter;
import in.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery;
import in.myinnos.awesomeimagepicker.models.Album;
import in.myinnos.awesomeimagepicker.models.MediaStoreType;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class AlbumSelectActivity extends HelperActivity {
    private ArrayList<Album> albums;

    private TextView errorDisplay, tvProfile, emptyMessageDisplay;
    private LinearLayout liFinish;

    private ProgressBar loader;
    private RecyclerView recyclerView;
    private AlbumSelectAdapter adapter;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] albumProjection = new String[]{
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
    };

    private final String[] mediaProjection = new String[] {
            MediaStore.MediaColumns._ID,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_album_select);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }

        ConstantsCustomGallery.limit = intent.getIntExtra(ConstantsCustomGallery.INTENT_EXTRA_LIMIT, ConstantsCustomGallery.DEFAULT_LIMIT);

        ConstantsCustomGallery.mediaStoreType = MediaStoreType.MIXED;
        if (intent.hasExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIASTORETYPE)) {
            MediaStoreType mediaStoreType = (MediaStoreType) intent.getSerializableExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIASTORETYPE);
            if (mediaStoreType != null) {
                ConstantsCustomGallery.mediaStoreType = mediaStoreType;
            }
        }

        setMessageDisplays();

        tvProfile = findViewById(R.id.tvProfile);
        tvProfile.setText(R.string.album_view);
        liFinish = findViewById(R.id.liFinish);

        loader = findViewById(R.id.loader);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(llm);

        liFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(abc_fade_in, abc_fade_out);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case ConstantsCustomGallery.PERMISSION_GRANTED: {
                        loadAlbums();
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_STARTED: {
                        loader.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_UPDATED: {
                        if (adapter == null) {

                            adapter = new AlbumSelectAdapter(AlbumSelectActivity.this, albums, ConstantsCustomGallery.mediaStoreType) {
                                @Override
                                public void clicked(int position, Album album) {

                                    Intent intent = new Intent(getApplicationContext(), MediaSelectActivity.class);
                                    intent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM, album.getName());
                                    intent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, album.getId());
                                    startActivityForResult(intent, ConstantsCustomGallery.REQUEST_CODE);
                                }
                            };

                            recyclerView.setAdapter(adapter);

                            loader.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);

                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        break;
                    }

                    case ConstantsCustomGallery.ERROR: {
                        loader.setVisibility(View.GONE);
                        errorDisplay.setVisibility(View.VISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.EMPTY_LIST: {
                        loader.setVisibility(View.GONE);
                        emptyMessageDisplay.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                loadAlbums();
            }
        };

        getContentResolver().registerContentObserver(ConstantsCustomGallery.getQueryUri(), false, observer);

        checkPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopThread();

        getContentResolver().unregisterContentObserver(observer);
        observer = null;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        albums = null;
//        if (adapter != null) {
//            adapter.releaseResources();
//        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        overridePendingTransition(abc_fade_in, abc_fade_out);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ConstantsCustomGallery.REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }

            default: {
                return false;
            }
        }
    }

    private void setMessageDisplays() {
        errorDisplay = findViewById(R.id.text_view_error);
        errorDisplay.setVisibility(View.INVISIBLE);

        String mediaTypeName;

        if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.VIDEOS) {
            mediaTypeName = getString(R.string.album_select_videos);
        }
        else if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.IMAGES) {
            mediaTypeName = getString(R.string.album_select_photos);
        }
        else {
            mediaTypeName = getString(R.string.album_select_media);
        }

        emptyMessageDisplay = findViewById(R.id.textViewEmpty);
        emptyMessageDisplay.setText(getString(R.string.activity_media_empty, mediaTypeName));
    }

    private void loadAlbums() {
        startThread(new AlbumLoaderRunnable());
    }

    private class AlbumLoaderRunnable implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            if (adapter == null) {
                sendMessage(ConstantsCustomGallery.FETCH_STARTED);
            }

            String selection = "";
            List<String> selectionArgs = new ArrayList<>();

            switch (ConstantsCustomGallery.mediaStoreType) {
                case MIXED:
                    selection += "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? OR "
                            + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?)";

                    selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
                    selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
                    break;
                case IMAGES:
                    selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
                    selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
                    break;
                case VIDEOS:
                    selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
                    selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
                    break;
            }

            Cursor cursor = getApplicationContext().getContentResolver().query(ConstantsCustomGallery.getQueryUri(), albumProjection,
                            selection, selectionArgs.toArray(new String[0]), MediaStore.Video.Media.DATE_MODIFIED);

            if (cursor == null) {
                sendMessage(ConstantsCustomGallery.ERROR);
                return;
            }

            int itemCount = 0;
            int pageCount = 50;

            if (albums == null) {
                albums = new ArrayList<>();
            }
            albums.clear();

            Album allPhotosAlbum = new Album(ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID, "All Photos", null);
            Album allVideosAlbum = new Album(ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID, "All Videos", null);

            switch (ConstantsCustomGallery.mediaStoreType) {
                case MIXED:
                    getThumbnailAndCount(allPhotosAlbum);
                    getThumbnailAndCount(allVideosAlbum);
                    albums.add(allPhotosAlbum);
                    albums.add(allVideosAlbum);
                    break;
                case IMAGES:
                    getThumbnailAndCount(allPhotosAlbum);
                    albums.add(allPhotosAlbum);
                    break;
                case VIDEOS:
                    getThumbnailAndCount(allVideosAlbum);
                    albums.add(allVideosAlbum);
                    break;
            }

            HashSet<Long> albumSet = new HashSet<>();
            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME));

                    if (!albumSet.contains(id)) {

                        Album album = new Album();
                        album.setId(id);
                        album.setName(name);

                        getThumbnailAndCount(album);

                        albums.add(album);

                        /*if (!album.equals("Hiding particular folder")) {
                            temp.add(new Album(album, image));
                        }*/
                        albumSet.add(id);
                    }

                    itemCount++;

                    /*
                     * This will show thumbnails every time 50 items are loaded
                     */
                    if (cursor.isFirst() || itemCount > pageCount) {
                        sendMessage(ConstantsCustomGallery.FETCH_UPDATED);
                        itemCount = 0;
                    }

                } while (cursor.moveToPrevious());
            }
            else {
                // If here no media to select from
                sendMessage(ConstantsCustomGallery.EMPTY_LIST);
            }
            cursor.close();

            // adding taking photo from camera option!
            /*albums.add(new Album(getString(R.string.capture_photo),
                    "https://image.freepik.com/free-vector/flat-white-camera_23-2147490625.jpg"));*/

            sendMessage(ConstantsCustomGallery.FETCH_COMPLETED);
        }
    }

    private void getThumbnailAndCount(Album album) {

        long albumId = album.getId();

        String selection = "";
        List<String> selectionArgs = new ArrayList<>();

        if (albumId != ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID &&
                albumId != ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID) {
            selection = MediaStore.MediaColumns.BUCKET_ID + " = ? AND ";
            selectionArgs.add(String.valueOf(albumId));
        }

        if (albumId == ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID ||
                ConstantsCustomGallery.mediaStoreType == MediaStoreType.IMAGES) {
            selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
        }
        else if (albumId == ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID ||
                ConstantsCustomGallery.mediaStoreType == MediaStoreType.VIDEOS) {
            selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
        }
        else {
            selection += "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?)";

            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
        }

        Cursor cursor = getContentResolver().query(ConstantsCustomGallery.getQueryUri(), mediaProjection,
                selection, selectionArgs.toArray(new String[0]), MediaStore.MediaColumns.DATE_ADDED);

        if (cursor == null) {
            sendMessage(ConstantsCustomGallery.ERROR);
            return;
        }

        if (cursor.moveToLast()) {
            do {
                if (Thread.interrupted()) {
                    return;
                }

                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));

                Uri uri = ContentUris.withAppendedId(ConstantsCustomGallery.getQueryUri(), id);

                album.setUri(uri);
                album.setCount(cursor.getCount());

                try {
                    getContentResolver().openFileDescriptor(uri, "r");
                } catch (Exception e) {
                    // File doesn't actually exist
                    continue;
                }

                // We only need one thumbnail and the count
                break;

            } while (cursor.moveToPrevious());
        }
        cursor.close();
    }

    private void startThread(Runnable runnable) {
        stopThread();
        thread = new Thread(runnable);
        thread.start();
    }

    private void stopThread() {
        if (thread == null || !thread.isAlive()) {
            return;
        }

        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(int what) {
        if (handler == null) {
            return;
        }

        Message message = handler.obtainMessage();
        message.what = what;
        message.sendToTarget();
    }

    @Override
    protected void permissionGranted() {
        Message message = handler.obtainMessage();
        message.what = ConstantsCustomGallery.PERMISSION_GRANTED;
        message.sendToTarget();
    }

    @Override
    protected void hideViews() {
        loader.setVisibility(View.GONE);
        recyclerView.setVisibility(View.INVISIBLE);
    }
}
