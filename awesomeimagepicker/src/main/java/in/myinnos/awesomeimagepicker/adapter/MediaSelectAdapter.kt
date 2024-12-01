package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.databinding.GridViewMediaSelectBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Video
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.Image
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.MediaType
import java.util.*
import java.util.concurrent.TimeUnit


abstract class MediaSelectAdapter(private val context: Context,
                                  private val album: Album) : RecyclerView.Adapter<MediaSelectAdapter.ViewHolder>() {

    abstract fun clicked(media: Media)
    abstract fun longClicked(media: Media)

    private var filteredMediaList = album.mediaList

    fun filterMedia(mediaType: MediaType) {
        filteredMediaList = when (mediaType) {
            MediaType.IMAGES -> album.mediaList.filter { it is Image }
            MediaType.VIDEOS -> album.mediaList.filter { it is Video }
            else -> album.mediaList
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = GridViewMediaSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val binding = holder.binding

        val media = filteredMediaList[position]

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

        // Only for android 11 and up
        binding.favorite.visibility = when (media.isFavorite) {
            true -> View.VISIBLE
            else -> View.GONE
        }

        if (media is Video) {

            if (media.duration != 0L) {

                val millis = media.duration
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                var seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                if (seconds > 59) {
                    seconds %= 60
                }
                val duration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                binding.videoDuration.visibility = View.VISIBLE
                binding.videoDuration.text = duration
            } else {
                binding.videoDuration.visibility = View.GONE
            }
        } else {
            binding.iconPlayView.visibility = View.GONE
            binding.videoDuration.visibility = View.GONE
        }

        /*
         * If the favorite icon is showing, or the video duration is showing, then
         * show the bottom black gradient.
         */
        binding.bottomGradient.visibility = View.GONE
        if (binding.favorite.visibility == View.VISIBLE || binding.videoDuration.visibility == View.VISIBLE) {
            binding.bottomGradient.visibility = View.VISIBLE
        }

        binding.root.setOnClickListener {
            clicked(media)
        }

        // Long click to see a preview of the image or video
        binding.root.setOnLongClickListener {
            longClicked(media)
            // Return true or it will also trigger an on click event
            true
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
        return filteredMediaList.size
    }

    class ViewHolder internal constructor(var binding: GridViewMediaSelectBinding) : RecyclerView.ViewHolder(binding.root)
}