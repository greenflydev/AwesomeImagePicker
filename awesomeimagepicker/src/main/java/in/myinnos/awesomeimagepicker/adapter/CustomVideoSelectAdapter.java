package in.myinnos.awesomeimagepicker.adapter;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.models.Video;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class CustomVideoSelectAdapter extends CustomGenericAdapter<Video> {

    public CustomVideoSelectAdapter(Activity activity, Context context, ArrayList<Video> videos) {
        super(activity, context, videos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        Video video = arrayList.get(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_view_video_select, null);

            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image_view_image_select);
            viewHolder.iconPlayView = (ImageView) convertView.findViewById(R.id.image_view_icon_play);
            viewHolder.videoDuration = (TextView) convertView.findViewById(R.id.text_view_duration);
            viewHolder.view = convertView.findViewById(R.id.view_alpha);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.getLayoutParams().width = size;
        viewHolder.imageView.getLayoutParams().height = size;

        viewHolder.view.getLayoutParams().width = size;
        viewHolder.view.getLayoutParams().height = size;

        if (arrayList.get(position).isSelected) {
            viewHolder.view.setAlpha(0.5f);
            ((FrameLayout) convertView).setForeground(context.getResources().getDrawable(R.drawable.ic_done_white));

        } else {
            viewHolder.view.setAlpha(0.0f);
            ((FrameLayout) convertView).setForeground(null);
        }

        if (video.duration != null && !video.duration.equals("")) {
            try {
                long millis = Long.parseLong(video.duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                String duration = String.format(Locale.getDefault(), "%d:%2d", minutes, seconds);

                viewHolder.videoDuration.setVisibility(View.VISIBLE);
                viewHolder.videoDuration.setText(duration);

            } catch (Exception e) {
                // skip it
            }
        }

        Uri uri = Uri.fromFile(new File(video.path));

        Glide.with(context).load(uri)
                .placeholder(R.color.colorAccent)
                .override(200, 200)
                .crossFade()
                .centerCrop()
                .into(viewHolder.imageView);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView imageView;
        public ImageView iconPlayView;
        public TextView videoDuration;
        public View view;
    }

}