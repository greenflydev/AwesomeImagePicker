package `in`.myinnos.awesomeimagepicker.views

import `in`.myinnos.awesomeimagepicker.databinding.MediaPreviewBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.Video
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

/**
 * This is the view that will display when the user long presses a media item
 * and will preview the image or video
 */
class MediaPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyle, defStyleRes) {

    private val binding = MediaPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    /*
     * Suppressing the complaints on the setOnTouchListener
     * "Custom view `VideoView` has setOnTouchListener called on it but does not override performClick"
     * This is required to be able to tap the video and dismiss it.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showMediaPreview(media: Media) {

        binding.root.visibility = VISIBLE
        binding.root.setOnClickListener { dismissMediaPreview() }

        binding.previewRoundedClip.clipToOutline = true

        if (media is Video) {

            binding.videoView.visibility = VISIBLE

            binding.videoView.setVideoURI(media.getUri())
            binding.videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.setVolume(0f, 0f)
                mediaPlayer.isLooping = true
                binding.videoView.start()
            }

            binding.videoView.setOnClickListener { dismissMediaPreview() }

            binding.videoView.setOnTouchListener { view, _ ->
                view.performClick()
                true
            }
        } else {

            Glide.with(this)
                .load(media.uri)
                .apply(RequestOptions.fitCenterTransform())
                .into(binding.imageView)

            binding.imageView.setOnClickListener { dismissMediaPreview() }
            binding.imageView.visibility = VISIBLE
        }

        broadcastLongPressPreview()
    }

    private fun dismissMediaPreview() {
        binding.root.visibility = GONE
        binding.imageView.setImageURI(null)
        binding.imageView.visibility = GONE
        binding.videoView.stopPlayback()
        binding.videoView.setVideoURI(null)
        binding.videoView.visibility = GONE
    }

    private fun broadcastLongPressPreview() {
        /*
         * This will broadcast out that the user long pressed to preview
         */
        val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
        localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_LONG_PRESS, true)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
    }
}
