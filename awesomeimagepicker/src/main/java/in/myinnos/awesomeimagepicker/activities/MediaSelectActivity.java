package in.myinnos.awesomeimagepicker.activities;

import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_in;
import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_out;

import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.adapter.CustomMediaSelectAdapter;
import in.myinnos.awesomeimagepicker.databinding.ActivityImageSelectBinding;
import in.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery;
import in.myinnos.awesomeimagepicker.models.Image;
import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.MediaStoreType;
import in.myinnos.awesomeimagepicker.models.Video;
import in.myinnos.awesomeimagepicker.views.CustomToolbar;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class MediaSelectActivity extends HelperActivity {

    private ActivityImageSelectBinding binding;

    private ArrayList<Media> mediaList;
    private long albumId;
    private MediaStoreType mediaStoreType;

    private CustomMediaSelectAdapter adapter;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] projectionVideos = new String[] {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
    };

    private final String[] projectionImages = new String[] {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageSelectBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        albumId = intent.getLongExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, 0);

        /*
         * This is the media type that comes from GF. Can be mixed, photos, or videos.
         *
         * We are storing it locally here because the user might have pickd all photos or all videos.
         * We don't want to overwrite the global type of media, such as mixed.
         *
         * Example:
         * Globally: The user can pick mixed
         * In the album list the user picked : All Photos
         * In this media list we only need to show mediaStoreType = MediaStoreType.IMAGES
         */
        mediaStoreType = ConstantsCustomGallery.mediaStoreType;

        /*
         * This happens if the user picks to see All Photos or All Videos.
         * We will switch the mediaStore type to just photos or videos.
         */
        if (albumId == ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID) {
            mediaStoreType = MediaStoreType.IMAGES;
        }
        else if (albumId == ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID) {
            mediaStoreType = MediaStoreType.VIDEOS;
        }

        int titleId = R.string.media_view;
        switch (mediaStoreType) {
            case VIDEOS:
                if (ConstantsCustomGallery.limit == 1) {
                    titleId = R.string.single_video_view;
                } else {
                    titleId = R.string.video_view;
                }
                break;
            case IMAGES:
                if (ConstantsCustomGallery.limit == 1) {
                    titleId = R.string.single_image_view;
                } else {
                    titleId = R.string.image_view;
                }
                break;
        }
        binding.toolbar.setTitle(getString(titleId, ConstantsCustomGallery.limit));

        binding.errorDisplay.setVisibility(View.INVISIBLE);

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

        setupTabLayout();

        /*
         * If the user changes between albums we need to show the selected count right away
         * along with the done button
         */
        displaySelectedCount();
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

    @Override
    protected void onStart() {
        super.onStart();

        handler = new Handler(msg -> {

            switch (msg.what) {
                case ConstantsCustomGallery.PERMISSION_GRANTED: {
                    loadMedia();
                    break;
                }

                case ConstantsCustomGallery.FETCH_STARTED: {
                    binding.loader.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.INVISIBLE);
                    break;
                }

                case ConstantsCustomGallery.FETCH_UPDATED: {
                    /*
                     * Every 50 items we will update the adapter.
                     */
                    if (adapter == null) {

                        adapter = new CustomMediaSelectAdapter(MediaSelectActivity.this, mediaList) {
                            @Override
                            public void clicked(int position) {

                                toggleSelection(position);

                                displaySelectedCount();
                            }
                        };
                        binding.recyclerView.setAdapter(adapter);

                        binding.loader.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.VISIBLE);
                        orientationBasedUI(getResources().getConfiguration().orientation);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    break;
                }

                case ConstantsCustomGallery.FETCH_COMPLETED: {
                    /*
                     * This will update the selected count
                     */
                    displaySelectedCount();

                    // Make sure the view updates, even if there are no results
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    break;
                }

                case ConstantsCustomGallery.ERROR: {
                    binding.loader.setVisibility(View.GONE);
                    binding.errorDisplay.setVisibility(View.VISIBLE);
                    break;
                }
            }
            return true;
        });
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                loadMedia();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false, observer);

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
        mediaList = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationBasedUI(newConfig.orientation);
    }

    /*
     * Sets up the tabs along the top, if the mode is mixed media.
     * The user can pick to see all, just photos, or just videos.
     */
    private void setupTabLayout() {

        if (mediaStoreType == MediaStoreType.MIXED) {

            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_all_media)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_photos)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_videos)));
            binding.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

            binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab != null) {
                        tabSelected(tab.getPosition());
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        } else {
            binding.tabLayout.setVisibility(View.GONE);
        }
    }

    /*
     * This will reload the media with the type of media selected
     */
    private void tabSelected(int position) {

        switch (position) {
            case ConstantsCustomGallery.TAB_ALL_MEDIA_POSITION:
                mediaStoreType = MediaStoreType.MIXED;
                break;
            case ConstantsCustomGallery.TAB_PHOTOS_POSITION:
                mediaStoreType = MediaStoreType.IMAGES;
                break;
            case ConstantsCustomGallery.TAB_VIDEOS_POSITION:
                mediaStoreType = MediaStoreType.VIDEOS;
                break;
        }

        /*
         * This will broadcast out that the user filtered to a media type.
         * Used for tracking in the calling application.
         */
        Intent localIntent = new Intent(ConstantsCustomGallery.BROADCAST_EVENT);
        localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_FILTER_BY_TYPE, true);
        localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_FILTER_BY_TYPE, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        loadMedia();
    }

    private void orientationBasedUI(int orientation) {
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 3 : 5;

        binding.recyclerView.setHasFixedSize(false);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
        binding.recyclerView.setLayoutManager(gridLayoutManager);
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

    private void toggleSelection(int position) {
        int countSelected = ConstantsCustomGallery.currentlySelectedMap.size();
        if (!mediaList.get(position).isSelected() && countSelected >= ConstantsCustomGallery.limit) {

            int messageId = R.string.media_limit_exceeded;
            switch (ConstantsCustomGallery.mediaStoreType) {
                case VIDEOS:
                    messageId = R.string.video_limit_exceeded;
                    break;
                case IMAGES:
                    messageId = R.string.image_limit_exceeded;
                    break;
            }

            Toast.makeText(
                    getApplicationContext(),
                    getString(messageId, ConstantsCustomGallery.limit),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Media media = mediaList.get(position);
        media.setSelected(!media.isSelected());
        if (media.isSelected()) {
            ConstantsCustomGallery.currentlySelectedMap.put(media.getId(), media);
        } else {
            ConstantsCustomGallery.currentlySelectedMap.remove(media.getId());
        }
        adapter.notifyDataSetChanged();
    }

    private void sendIntent() {

        ConstantsCustomGallery.previouslySelectedIds.addAll(ConstantsCustomGallery.currentlySelectedMap.keySet());

        ArrayList<Media> selectedVideos = new ArrayList<>();
        for (Map.Entry<Long, Media> entrySet : ConstantsCustomGallery.currentlySelectedMap.entrySet()) {
            selectedVideos.add(entrySet.getValue());
        }

        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA, selectedVideos);
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(abc_fade_in, abc_fade_out);
    }

    private void loadMedia() {
        startThread(new MediaLoaderRunnable());
    }

    private class MediaLoaderRunnable implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            /*
             * If the adapter is null, this is first time this activity's view is
             * being shown, hence send FETCH_STARTED message to show progress bar
             * while videos are loaded from phone
             */
            if (adapter == null) {
                sendMessage(ConstantsCustomGallery.FETCH_STARTED);
            }

            String selection = "";
            List<String> selectionArgs = new ArrayList<>();

            String[] projection = projectionVideos;
            if (mediaStoreType == MediaStoreType.IMAGES) {
                projection = projectionImages;
            }

            /*
             * Only real albums need to query by bucket id (album id)
             */
            if (albumId != ConstantsCustomGallery.ALL_PHOTOS_ALBUM_ID &&
                    albumId != ConstantsCustomGallery.ALL_VIDEOS_ALBUM_ID) {
                selection = MediaStore.MediaColumns.BUCKET_ID + " = ? AND ";
                selectionArgs.add(String.valueOf(albumId));
            }

            /*
             * Query the right kind of media, depending on mixed/photos/videos
             */
            switch (mediaStoreType) {
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

            Cursor cursor = getContentResolver().query(ConstantsCustomGallery.getQueryUri(),
                    projection, selection, selectionArgs.toArray(new String[0]), MediaStore.MediaColumns.DATE_ADDED);
            if (cursor == null) {
                sendMessage(ConstantsCustomGallery.ERROR);
                return;
            }

            int itemCount = 0;
            int pageCount = 50;

            if (mediaList == null) {
                mediaList = new ArrayList<>();
            }
            mediaList.clear();

            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    long duration = 0;
                    if (mediaStoreType != MediaStoreType.IMAGES) {
                        duration = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DURATION));
                    }
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
                    String mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));

                    Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    if (mimeType.startsWith("image")) {
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    }

                    try {
                        getContentResolver().openFileDescriptor(uri, "r");
                    } catch (Exception e) {
                        // File doesn't actually exist
                        continue;
                    }

                    boolean isSelected = ConstantsCustomGallery.currentlySelectedMap.containsKey(id);

                    if (mimeType.startsWith("image")) {
                        Image image = new Image();
                        image.setId(id);
                        image.setName(name);
                        image.setSize(size);
                        image.setMimeType(mimeType);
                        image.setUri(uri);
                        image.setSelected(isSelected);
                        mediaList.add(image);
                    } else {
                        Video video = new Video();
                        video.setId(id);
                        video.setName(name);
                        video.setSize(size);
                        video.setMimeType(mimeType);
                        video.setDuration(duration);
                        video.setUri(uri);
                        video.setSelected(isSelected);
                        mediaList.add(video);
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
            cursor.close();

            /*
             * In case this runnable is executed to onChange calling loadMedia,
             * using countSelected variable can result in a race condition. To avoid that,
             * tempCountSelected keeps track of number of selected videos. On handling
             * FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */
            int tempCountSelected = 0;
            for (Media m : mediaList) {
                if (ConstantsCustomGallery.currentlySelectedMap.containsKey(m.getId())) {
                    tempCountSelected++;
                    m.setSelected(true);
                }
                else if (m.isSelected()) {
                    tempCountSelected++;
                }
            }

            sendMessage(ConstantsCustomGallery.FETCH_COMPLETED, tempCountSelected);
        }
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
        sendMessage(what, 0);
    }

    private void sendMessage(int what, int arg1) {
        if (handler == null) {
            return;
        }

        Message message = handler.obtainMessage();
        message.what = what;
        message.arg1 = arg1;
        message.sendToTarget();
    }

    @Override
    protected void permissionGranted() {
        sendMessage(ConstantsCustomGallery.PERMISSION_GRANTED);
    }

    @Override
    protected void hideViews() {
        binding.loader.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(abc_fade_in, abc_fade_out);
        finish();
    }
}
