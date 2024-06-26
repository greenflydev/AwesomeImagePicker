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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.adapter.AlbumSelectAdapter;
import in.myinnos.awesomeimagepicker.databinding.ActivityAlbumSelectBinding;
import in.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery;
import in.myinnos.awesomeimagepicker.models.Album;
import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.MediaStoreType;
import in.myinnos.awesomeimagepicker.views.CustomToolbar;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class AlbumSelectActivity extends HelperActivity {

    private ArrayList<Album> albums;

    private ActivityAlbumSelectBinding binding;

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

        binding = ActivityAlbumSelectBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

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

        binding.toolbar.setTitle(getString(R.string.album_view));

        binding.recyclerView.setHasFixedSize(false);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(RecyclerView.VERTICAL);
        binding.recyclerView.setLayoutManager(llm);

        binding.toolbar.setCallback(new CustomToolbar.Callback() {
            @Override
            public void onBack() {
                finish();
                overridePendingTransition(abc_fade_in, abc_fade_out);
            }

            @Override
            public void onDone() {
                sendIntent();
            }
        });

        /*
         * Whenever this is loaded for the first time, clear out the list of selected items.
         * Only clear it here because the user should be able to go between albums and select
         * more items.
         */
        ConstantsCustomGallery.currentlySelectedMap.clear();
    }

    private void displaySelectedCount() {

        int selectedCount = ConstantsCustomGallery.currentlySelectedMap.size();
        if (selectedCount == 0) {
            binding.selectedCount.setVisibility(View.GONE);
            binding.toolbar.showDone(false);
        } else {
            String itemSelected = getString(R.string.item_selected, selectedCount);
            if (selectedCount > 1) {
                itemSelected = getString(R.string.items_selected, selectedCount);
            }
            binding.selectedCount.setText(itemSelected);
            binding.selectedCount.setVisibility(View.VISIBLE);
            binding.toolbar.showDone(true);
        }
    }

    private void sendIntent() {


        ConstantsCustomGallery.getPreviouslySelectedIds(this).addAll(ConstantsCustomGallery.currentlySelectedMap.keySet());
        ConstantsCustomGallery.savePreviouslySelectedIds(this);

        ArrayList<Media> selectedVideos = new ArrayList<>();
        for (Map.Entry<String, Media> entrySet : ConstantsCustomGallery.currentlySelectedMap.entrySet()) {
            selectedVideos.add(entrySet.getValue());
        }

        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA, selectedVideos);
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(abc_fade_in, abc_fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * If the user changes between albums we need to show the selected count right away
         * along with the done button
         */
        displaySelectedCount();
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
                        binding.loader.setVisibility(View.VISIBLE);
                        binding.recyclerView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_UPDATED: {
                        if (adapter == null) {

                            adapter = new AlbumSelectAdapter(AlbumSelectActivity.this, albums) {
                                @Override
                                public void clicked(int position, Album album) {

                                    /*
                                     * This will broadcast out that the user selected an album.
                                     * Used for tracking in the calling application.
                                     */
                                    Intent localIntent = new Intent(ConstantsCustomGallery.BROADCAST_EVENT);
                                    localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_ALBUM_SELECTED, true);
                                    localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, album.getId());
                                    LocalBroadcastManager.getInstance(AlbumSelectActivity.this).sendBroadcast(localIntent);

                                    Intent intent = new Intent(getApplicationContext(), MediaSelectActivity.class);
                                    intent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM, album.getName());
                                    intent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, album.getId());
                                    startActivityForResult(intent, ConstantsCustomGallery.REQUEST_CODE);
                                }
                            };

                            binding.recyclerView.setAdapter(adapter);

                            binding.loader.setVisibility(View.GONE);
                            binding.recyclerView.setVisibility(View.VISIBLE);

                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        break;
                    }

                    case ConstantsCustomGallery.ERROR: {
                        binding.loader.setVisibility(View.GONE);
                        binding.errorDisplay.setVisibility(View.VISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.EMPTY_LIST: {
                        binding.loader.setVisibility(View.GONE);
                        binding.emptyView.setVisibility(View.VISIBLE);
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

        binding.errorDisplay.setVisibility(View.INVISIBLE);

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

        binding.emptyView.setText(getString(R.string.activity_media_empty, mediaTypeName));
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

            /*
             * Depending on the type of media allowed, select the right albums.
             * Mixed : Show albums with either photos or videos
             * Photo/Video : Show albums with photos or videos
             */
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

            /*
             * These are fake albums for displaying all photos or all videos.
             * The thumbnail will be the most recent photo or video on the device.
             */
            Album allPhotosAlbum = new Album(ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID, getString(R.string.all_photos), null);
            Album allVideosAlbum = new Album(ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID, getString(R.string.all_videos), null);

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

    /*
     * For each album we need to get the thumbnail and also the count (number of items in the album)
     */
    private void getThumbnailAndCount(Album album) {

        long albumId = album.getId();

        String selection = "";
        List<String> selectionArgs = new ArrayList<>();

        /*
         * Only real albums need to query by a bucket id (the album id)
         */
        if (albumId != ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID &&
                albumId != ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID) {
            selection = MediaStore.MediaColumns.BUCKET_ID + " = ? AND ";
            selectionArgs.add(String.valueOf(albumId));
        }

        /*
         * If the media type is only images OR if this is the fake images album
         * then grab the latest thumbnail for just images
         */
        if (albumId == ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID ||
                ConstantsCustomGallery.mediaStoreType == MediaStoreType.IMAGES) {
            selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
        }
        /*
         * If the media type is only videos OR if this is the fake videos album
         * then grab the latest thumbnail for just videos
         */
        else if (albumId == ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID ||
                ConstantsCustomGallery.mediaStoreType == MediaStoreType.VIDEOS) {
            selection += MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?";
            selectionArgs.add(String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
        }
        /*
         * If we get here, then the media type is mixed. So get the thumbnail for either photos or videos
         */
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
                album.setCount(cursor.getCount()); // This is the number of mixed/photos/videos in the album

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
        binding.loader.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.INVISIBLE);
    }
}
