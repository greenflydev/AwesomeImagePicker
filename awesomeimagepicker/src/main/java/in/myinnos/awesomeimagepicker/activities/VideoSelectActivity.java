package in.myinnos.awesomeimagepicker.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.adapter.CustomVideoSelectAdapter;
import in.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery;
import in.myinnos.awesomeimagepicker.models.Video;

import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_in;
import static in.myinnos.awesomeimagepicker.R.anim.abc_fade_out;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class VideoSelectActivity extends HelperActivity {
    private ArrayList<Video> videos;
    private String album;

    private TextView errorDisplay, tvProfile, tvAdd, tvSelectCount;
    private LinearLayout liFinish;

    private ProgressBar loader;
    private GridView gridView;
    private CustomVideoSelectAdapter adapter;


    private int countSelected;

    private ContentObserver observer;
    private Handler handler;
    private Thread thread;

    private final String[] projection =
            new String[] {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATA };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_select);
        setView(findViewById(R.id.layout_image_select));

        tvProfile = (TextView) findViewById(R.id.tvProfile);
        tvAdd = (TextView) findViewById(R.id.tvAdd);
        tvSelectCount = (TextView) findViewById(R.id.tvSelectCount);
        tvProfile.setText(R.string.video_view);
        liFinish = (LinearLayout) findViewById(R.id.liFinish);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        album = intent.getStringExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM);

        errorDisplay = (TextView) findViewById(R.id.text_view_error);
        errorDisplay.setVisibility(View.INVISIBLE);

        loader = (ProgressBar) findViewById(R.id.loader);
        gridView = (GridView) findViewById(R.id.grid_view_image_select);
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

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ConstantsCustomGallery.PERMISSION_GRANTED: {
                        loadImages();
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_STARTED: {
                        loader.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case ConstantsCustomGallery.FETCH_COMPLETED: {
                        /*
                        If adapter is null, this implies that the loaded videos will be shown
                        for the first time, hence send FETCH_COMPLETED message.
                        However, if adapter has been initialised, this thread was run either
                        due to the activity being restarted or content being changed.
                         */
                        if (adapter == null) {
                            adapter = new CustomVideoSelectAdapter(VideoSelectActivity.this, getApplicationContext(), videos);
                            gridView.setAdapter(adapter);

                            loader.setVisibility(View.GONE);
                            gridView.setVisibility(View.VISIBLE);
                            orientationBasedUI(getResources().getConfiguration().orientation);

                        } else {
                            adapter.notifyDataSetChanged();
                            /*
                            Some selected videos may have been deleted
                            hence update action mode title
                             */
                            countSelected = msg.arg1;
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

                    default: {
                        super.handleMessage(msg);
                    }
                }
            }
        };
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                loadImages();
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

        videos = null;
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
        if (!videos.get(position).isSelected && countSelected >= ConstantsCustomGallery.limit) {
            Toast.makeText(
                    getApplicationContext(),
                    String.format(getString(R.string.video_limit_exceeded), ConstantsCustomGallery.limit),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        videos.get(position).isSelected = !videos.get(position).isSelected;
        if (videos.get(position).isSelected) {
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

        for (int i = 0, l = videos.size(); i < l; i++) {
            videos.get(i).isSelected = false;
        }
        countSelected = 0;
        adapter.notifyDataSetChanged();
    }

    private ArrayList<Video> getSelected() {
        ArrayList<Video> selectedVideos = new ArrayList<>();
        for (int i = 0, l = videos.size(); i < l; i++) {
            if (videos.get(i).isSelected) {
                selectedVideos.add(videos.get(i));
            }
        }
        return selectedVideos;
    }

    private void sendIntent() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_VIDEOS, getSelected());
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(abc_fade_in, abc_fade_out);
    }

    private void loadImages() {
        startThread(new ImageLoaderRunnable());
    }

    private class ImageLoaderRunnable implements Runnable {
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

            File file;
            HashSet<Long> selectedVideos = new HashSet<>();
            if (videos != null) {
                Video video;
                for (int i = 0, l = videos.size(); i < l; i++) {
                    video = videos.get(i);
                    file = new File(video.path);
                    if (file.exists() && video.isSelected) {
                        selectedVideos.add(video.id);
                    }
                }
            }

            Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " =?", new String[]{album}, MediaStore.Video.Media.DATE_ADDED);
            if (cursor == null) {
                sendMessage(ConstantsCustomGallery.ERROR);
                return;
            }

            /*
            In case this runnable is executed to onChange calling loadImages,
            using countSelected variable can result in a race condition. To avoid that,
            tempCountSelected keeps track of number of selected videos. On handling
            FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */
            int tempCountSelected = 0;
            ArrayList<Video> temp = new ArrayList<>(cursor.getCount());
            if (cursor.moveToLast()) {
                do {
                    if (Thread.interrupted()) {
                        return;
                    }

                    long id = cursor.getLong(cursor.getColumnIndex(projection[0]));
                    String name = cursor.getString(cursor.getColumnIndex(projection[1]));
                    String duration = cursor.getString(cursor.getColumnIndex(projection[2]));
                    String path = cursor.getString(cursor.getColumnIndex(projection[3]));
                    boolean isSelected = selectedVideos.contains(id);
                    if (isSelected) {
                        tempCountSelected++;
                    }

                    file = null;
                    try {
                        file = new File(path);
                    } catch (Exception e) {
                        Log.d("Exception : ", e.toString());
                    }

                    if (file.exists()) {
                        temp.add(new Video(id, name, duration, path, isSelected));
                    }

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            if (videos == null) {
                videos = new ArrayList<>();
            }
            videos.clear();
            videos.addAll(temp);

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
