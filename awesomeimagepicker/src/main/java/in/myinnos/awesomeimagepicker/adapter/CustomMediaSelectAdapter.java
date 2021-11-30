package in.myinnos.awesomeimagepicker.adapter;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import in.myinnos.awesomeimagepicker.R;
import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.Video;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class CustomMediaSelectAdapter extends CustomGenericAdapter<Media> {

    public CustomMediaSelectAdapter(Activity activity, Context context, ArrayList<Media> media) {
        super(activity, context, media);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        Media media = arrayList.get(position);

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_view_media_select, null);

            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.image_view_image_select);
            viewHolder.iconPlayView = convertView.findViewById(R.id.image_view_icon_play);
            viewHolder.videoDuration = convertView.findViewById(R.id.text_view_duration);
            viewHolder.view = convertView.findViewById(R.id.view_alpha);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.imageView.getLayoutParams().width = size;
        viewHolder.imageView.getLayoutParams().height = size;

        viewHolder.view.getLayoutParams().width = size;
        viewHolder.view.getLayoutParams().height = size;

        if (arrayList.get(position).isSelected()) {
            viewHolder.view.setAlpha(0.5f);
            ((FrameLayout) convertView).setForeground(context.getResources().getDrawable(R.drawable.ic_done_white));

        } else {
            viewHolder.view.setAlpha(0.0f);
            ((FrameLayout) convertView).setForeground(null);
        }

        if (media instanceof Video) {

            Video video = (Video) media;

            //viewHolder.iconPlayView.setVisibility(View.VISIBLE);

            if (video.getDuration() != 0) {
                long millis = video.getDuration();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                if (seconds > 59) {
                    seconds = seconds % 60;
                }
                String duration = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);

                viewHolder.videoDuration.setVisibility(View.VISIBLE);
                viewHolder.videoDuration.setText(duration);
            } else {
                viewHolder.videoDuration.setVisibility(View.GONE);
            }
        } else {
            viewHolder.iconPlayView.setVisibility(View.GONE);
            viewHolder.videoDuration.setVisibility(View.GONE);
        }

        Uri uri = media.getUri();

        Glide.with(context)
                .load(uri)
                .apply(RequestOptions.placeholderOf(new ColorDrawable(0xFFf2f2f2)))
                .apply(RequestOptions.overrideOf(200, 200))
                .apply(RequestOptions.centerCropTransform())
                .transition(withCrossFade())
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
