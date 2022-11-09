package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.databinding.GridViewMediaSelectBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.Video
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.util.*
import java.util.concurrent.TimeUnit


abstract class CustomMediaSelectAdapter(private val context: Context,
                                        private val mediaList: List<Media>) : RecyclerView.Adapter<CustomMediaSelectAdapter.ViewHolder>() {

    abstract fun clicked(position: Int)
    abstract fun previewVideo(media: Media)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = GridViewMediaSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val binding = holder.binding

        val media = mediaList[position]

        if (media.isSelected) {
            binding.viewAlpha.visibility = View.VISIBLE
            binding.selected.visibility = View.VISIBLE
        } else {
            binding.viewAlpha.visibility = View.GONE
            binding.selected.visibility = View.GONE
        }

        // Show if the user has already uploaded this media item
        if (ConstantsCustomGallery.getPreviouslySelectedIds(context).contains(media.id.toString())) {
            binding.previouslySelected.visibility = View.VISIBLE
        } else {
            binding.previouslySelected.visibility = View.GONE
        }

        if (media is Video) {

            if (media.duration != 0L) {

                val millis = media.duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                var seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                if (seconds > 59) {
                    seconds %= 60
                }
                val duration = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)

                binding.videoDuration.visibility = View.VISIBLE
                binding.videoDuration.text = duration
            } else {
                binding.videoDuration.visibility = View.GONE
            }
        } else {
            binding.iconPlayView.visibility = View.GONE
            binding.videoDuration.visibility = View.GONE
        }

        binding.root.setOnClickListener { clicked(position) }

        if (media is Video) {
            binding.root.setOnLongClickListener {
                previewVideo(media)
                true
            }
        } else {
            binding.root.setOnLongClickListener {
                true
            }
        }

        val uri = media.uri

        Glide.with(context)
                .load(uri)
                .apply(RequestOptions.placeholderOf(ColorDrawable(0xFFf2f2f2.toInt())))
                .apply(RequestOptions.overrideOf(200, 200))
                .apply(RequestOptions.centerCropTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageView)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    class ViewHolder internal constructor(var binding: GridViewMediaSelectBinding) : RecyclerView.ViewHolder(binding.root)
}