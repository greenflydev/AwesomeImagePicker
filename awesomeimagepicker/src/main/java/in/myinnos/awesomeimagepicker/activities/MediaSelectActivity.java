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
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.adapter.CustomMediaSelectAdapter;
import in.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery;
import in.myinnos.awesomeimagepicker.models.Image;
import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.MediaStoreType;
import in.myinnos.awesomeimagepicker.models.Video;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class MediaSelectActivity extends HelperActivity {

    private ArrayList<Media> media;
    private String album;
    private long albumId;

    private TextView errorDisplay, tvProfile, tvAdd, tvSelectCount;
    private LinearLayout liFinish;

    private ProgressBar loader;
    private GridView gridView;
    private CustomMediaSelectAdapter adapter;

    private int countSelected;

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
        setContentView(R.layout.activity_image_select);
        setView(findViewById(R.id.layout_image_select));

        tvProfile = findViewById(R.id.tvProfile);
        tvAdd = findViewById(R.id.tvAdd);
        tvSelectCount = findViewById(R.id.tvSelectCount);
        liFinish = findViewById(R.id.liFinish);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        album = intent.getStringExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM);
        albumId = intent.getLongExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, 0);

        int profile = R.string.media_view;
        switch (ConstantsCustomGallery.mediaStoreType) {
            case VIDEOS:
                if (ConstantsCustomGallery.limit == 1) {
                    profile = R.string.single_video_view;
                } else {
                    profile = R.string.video_view;
                }
                break;
            case IMAGES:
                if (ConstantsCustomGallery.limit == 1) {
                    profile = R.string.single_image_view;
                } else {
                    profile = R.string.image_view;
                }
                break;
        }
        tvProfile.setText(String.format(Locale.ENGLISH, getString(profile), ConstantsCustomGallery.limit));

        errorDisplay = findViewById(R.id.text_view_error);
        errorDisplay.setVisibility(View.INVISIBLE);

        loader = findViewById(R.id.loader);
        gridView = findViewById(R.id.grid_view_image_select);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                toggleSelection(position);
                //actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                tvSelectCount.setText(countSelected + " " + getString(R.string.selected));
                tvSelectCount.setVisibility(View.VISIBLE);
                tvAdd.setVisibility(View.VISIBLE);
                tvProfile.setVisibility(View.GONE);

                if (countSelected == 0) {
                    //actionMode.finish();
                    tvSelectCount.setVisibility(View.GONE);
                    tvAdd.setVisibility(View.GONE);
                    tvProfile.setVisibility(View.VISIBLE);
                }
            }
        });

        liFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvSelectCount.getVisibility() == View.VISIBLE) {
                    deselectAll();
                } else {
                    finish();
                    overridePendingTransition(abc_fade_in, abc_fade_out);
                }
            }
        });

        tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIntent();
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
                        loadMedia();
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_STARTED: {
                        loader.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_UPDATED: {
                        /*
                         * Every 50 items we will update the adapter.
                         */
                        if (adapter == null) {
                            adapter = new CustomMediaSelectAdapter(MediaSelectActivity.this, getApplicationContext(), media);
                            gridView.setAdapter(adapter);

                            loader.setVisibility(View.GONE);
                            gridView.setVisibility(View.VISIBLE);
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
                        countSelected = msg.arg1;
                        if (countSelected > 0) {
                            //actionMode.setTitle(countSelected + " " + getString(R.string.selected));
                            tvSelectCount.setText(countSelected + " " + getString(R.string.selected));
                            tvSelectCount.setVisibility(View.VISIBLE);
                            tvAdd.setVisibility(View.VISIBLE);
                            tvProfile.setVisibility(View.GONE);
                        }
                        break;
                    }

                    case ConstantsCustomGallery.ERROR: {
                        loader.setVisibility(View.GONE);
                        errorDisplay.setVisibility(View.VISIBLE);
                        break;
                    }
                }
                return true;
            }
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

        media = null;
        if (adapter != null) {
            adapter.releaseResources();
        }
        gridView.setOnItemClickListener(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationBasedUI(newConfig.orientation);
    }

    private void orientationBasedUI(int orientation) {
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        if (adapter != null) {
            int size = orientation == Configuration.ORIENTATION_PORTRAIT ? metrics.widthPixels / 3 : metrics.widthPixels / 5;
            adapter.setLayoutParams(size);
        }
        gridView.setNumColumns(orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5);
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
        if (!media.get(position).isSelected() && countSelected >= ConstantsCustomGallery.limit) {

            int message = R.string.media_limit_exceeded;
            switch (ConstantsCustomGallery.mediaStoreType) {
                case VIDEOS:
                    message = R.string.video_limit_exceeded;
                    break;
                case IMAGES:
                    message = R.string.image_limit_exceeded;
                    break;
            }

            Toast.makeText(
                    getApplicationContext(),
                    String.format(getString(message), ConstantsCustomGallery.limit),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        media.get(position).setSelected(!media.get(position).isSelected());
        if (media.get(position).isSelected()) {
            countSelected++;
        } else {
            countSelected--;
        }
        adapter.notifyDataSetChanged();
    }

    private void deselectAll() {
        tvProfile.setVisibility(View.VISIBLE);
        tvAdd.setVisibility(View.GONE);
        tvSelectCount.setVisibility(View.GONE);

        for (int i = 0, l = media.size(); i < l; i++) {
            media.get(i).setSelected(false);
        }
        countSelected = 0;
        adapter.notifyDataSetChanged();
    }

    private ArrayList<Media> getSelected() {
        ArrayList<Media> selectedVideos = new ArrayList<>();
        for (int i = 0, l = media.size(); i < l; i++) {
            if (media.get(i).isSelected()) {
                selectedVideos.add(media.get(i));
            }
        }
        return selectedVideos;
    }

    private void sendIntent() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA, getSelected());
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
            If the adapter is null, this is first time this activity's view is
            being shown, hence send FETCH_STARTED message to show progress bar
            while videos are loaded from phone
             */
            if (adapter == null) {
                sendMessage(ConstantsCustomGallery.FETCH_STARTED);
            }

            HashSet<Long> selectedMedia = new HashSet<>();
            if (media != null) {
                for (Media m : media) {
                    if (m.isSelected()) {
                        selectedMedia.add(m.getId());
                    }
                }
            }

            String selection = MediaStore.MediaColumns.BUCKET_ID + " = ?";
            String[] selectionArgs = new String[] {"" + albumId};

            String[] projection = projectionVideos;
            if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.IMAGES) {
                projection = projectionImages;
            }

            if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.MIXED) {
                selection += " AND (" + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? OR "
                        + MediaStore.Files.FileColumns.MEDIA_TYPE + " = ?)";

                selectionArgs = new String[] {
                        "" + albumId,
                        "" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        "" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE};
            }

            Cursor cursor = getContentResolver().query(ConstantsCustomGallery.getQueryUri(),
                    projection, selection, selectionArgs, MediaStore.MediaColumns.DATE_ADDED);
            if (cursor == null) {
                sendMessage(ConstantsCustomGallery.ERROR);
                return;
            }

            int itemCount = 0;
            int pageCount = 50;

            if (media == null) {
                media = new ArrayList<>();
            }
            media.clear();

            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    long duration = 0;
                    if (ConstantsCustomGallery.mediaStoreType != MediaStoreType.IMAGES) {
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

                    boolean isSelected = selectedMedia.contains(id);

                    if (mimeType.startsWith("image")) {
                        Image image = new Image();
                        image.setId(id);
                        image.setName(name);
                        image.setSize(size);
                        image.setMimeType(mimeType);
                        image.setUri(uri);
                        image.setSelected(isSelected);
                        media.add(image);
                    } else {
                        Video video = new Video();
                        video.setId(id);
                        video.setName(name);
                        video.setSize(size);
                        video.setMimeType(mimeType);
                        video.setDuration(duration);
                        video.setUri(uri);
                        video.setSelected(isSelected);
                        media.add(video);
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
            In case this runnable is executed to onChange calling loadMedia,
            using countSelected variable can result in a race condition. To avoid that,
            tempCountSelected keeps track of number of selected videos. On handling
            FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */
            int tempCountSelected = 0;
            for (Media m : media) {
                if (selectedMedia.contains(m.getId())) {
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
        loader.setVisibility(View.GONE);
        gridView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (tvSelectCount.getVisibility() == View.VISIBLE) {
            deselectAll();
        } else {
            super.onBackPressed();
            overridePendingTransition(abc_fade_in, abc_fade_out);
            finish();
        }

    }
}
