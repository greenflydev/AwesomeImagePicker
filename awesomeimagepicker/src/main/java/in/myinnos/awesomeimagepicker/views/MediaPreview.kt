package `in`.myinnos.awesomeimagepicker.views

import `in`.myinnos.awesomeimagepicker.databinding.MediaPreviewBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.Video
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.OneShotPreDrawListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

/**
 * This is the view that will display when the user long presses a media item
 * and will preview the image or video.
 *
 * The preview animates out of the thumbnail that was long pressed (growing from
 * its on-screen position to a centered full-size preview) and shrinks back into
 * it on dismiss, mimicking the iOS peek animation.
 */
class MediaPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyle, defStyleRes) {

    private val binding = MediaPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    /** On-screen bounds of the thumbnail the preview should grow out of / shrink back into. */
    private val startRect = Rect()

    /** Guards against the enter animation firing more than once per show. */
    private var enterListener: OneShotPreDrawListener? = null

    private var scrimAnimator: ValueAnimator? = null

    private val interpolator = FastOutSlowInInterpolator()

    /*
     * Suppressing the complaints on the setOnTouchListener
     * "Custom view `VideoView` has setOnTouchListener called on it but does not override performClick"
     * This is required to be able to tap the video and dismiss it.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showMediaPreview(media: Media, sourceView: View? = null) {

        captureStartRect(sourceView)

        binding.root.visibility = VISIBLE
        binding.root.setOnClickListener { dismissMediaPreview() }

        // Start the scrim transparent and the preview hidden so the first frame
        // doesn't flash at full size before the enter animation positions it.
        setScrimFraction(0f)
        binding.previewRoundedClip.alpha = 0f
        binding.previewRoundedClip.clipToOutline = true

        if (media is Video) {

            binding.videoView.visibility = VISIBLE

            binding.videoView.setVideoURI(media.uri)
            binding.videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.setVolume(0f, 0f)
                mediaPlayer.isLooping = true
                binding.videoView.start()
                // The VideoView only knows its size once prepared, so wait for it.
                animateEnterWhenLaidOut()
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

            // The image drives the preview height (adjustViewBounds), so wait for layout.
            animateEnterWhenLaidOut()
        }

        broadcastLongPressPreview()
    }

    private fun dismissMediaPreview() {
        enterListener?.removeListener()
        enterListener = null
        val preview = binding.previewRoundedClip

        // If we never captured a valid source (or the preview isn't laid out yet)
        // just fade out without the shrink.
        val transform = if (startRect.isEmpty || preview.width == 0) {
            null
        } else {
            transformInto(preview, startRect)
        }

        animateScrim(0f)

        val onEnd = Runnable { finishDismiss() }

        if (transform == null) {
            preview.animate()
                .alpha(0f)
                .setDuration(EXIT_DURATION)
                .setInterpolator(interpolator)
                .withEndAction(onEnd)
                .start()
        } else {
            preview.pivotX = preview.width / 2f
            preview.pivotY = preview.height / 2f
            preview.animate()
                .scaleX(transform.scale)
                .scaleY(transform.scale)
                .translationX(transform.translationX)
                .translationY(transform.translationY)
                .alpha(0f)
                .setDuration(EXIT_DURATION)
                .setInterpolator(interpolator)
                .withEndAction(onEnd)
                .start()
        }
    }

    private fun finishDismiss() {
        binding.root.visibility = GONE
        binding.imageView.setImageURI(null)
        binding.imageView.visibility = GONE
        binding.videoView.stopPlayback()
        binding.videoView.setVideoURI(null)
        binding.videoView.visibility = GONE

        // Reset the preview transform so the next show starts clean.
        binding.previewRoundedClip.apply {
            scaleX = 1f
            scaleY = 1f
            translationX = 0f
            translationY = 0f
            alpha = 1f
        }
    }

    /**
     * Snapshots the on-screen bounds of the long-pressed thumbnail so the preview
     * can animate from/to it. Clears the rect if no valid source is provided.
     */
    private fun captureStartRect(sourceView: View?) {
        if (sourceView == null || sourceView.width == 0 || sourceView.height == 0) {
            startRect.setEmpty()
            return
        }
        val location = IntArray(2)
        sourceView.getLocationOnScreen(location)
        startRect.set(
            location[0],
            location[1],
            location[0] + sourceView.width,
            location[1] + sourceView.height
        )
    }

    /**
     * Runs the enter animation once the preview has a real size. The image/video
     * content determines the final height, so we wait for the next layout pass.
     */
    private fun animateEnterWhenLaidOut() {
        enterListener?.removeListener()
        enterListener = OneShotPreDrawListener.add(binding.previewRoundedClip) {
            enterListener = null
            animateEnter()
        }
    }

    private fun animateEnter() {
        val preview = binding.previewRoundedClip

        animateScrim(1f)

        if (startRect.isEmpty || preview.width == 0) {
            // No source to grow from: fall back to a gentle scale + fade.
            preview.pivotX = preview.width / 2f
            preview.pivotY = preview.height / 2f
            preview.scaleX = FALLBACK_START_SCALE
            preview.scaleY = FALLBACK_START_SCALE
            preview.alpha = 0f
            preview.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(ENTER_DURATION)
                .setInterpolator(interpolator)
                .start()
            return
        }

        val transform = transformInto(preview, startRect)
        preview.pivotX = preview.width / 2f
        preview.pivotY = preview.height / 2f
        preview.scaleX = transform.scale
        preview.scaleY = transform.scale
        preview.translationX = transform.translationX
        preview.translationY = transform.translationY
        preview.alpha = 1f

        preview.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(ENTER_DURATION)
            .setInterpolator(interpolator)
            .start()
    }

    /**
     * Computes the scale + translation that maps [view] (at its natural, centered
     * position) onto [target]. Scaling is uniform (based on width) with centers
     * aligned, so the image grows out of the thumbnail without distorting.
     */
    private fun transformInto(view: View, target: Rect): Transform {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewCenterX = location[0] + view.width / 2f
        val viewCenterY = location[1] + view.height / 2f
        val scale = target.width().toFloat() / view.width
        return Transform(
            scale = scale,
            translationX = target.exactCenterX() - viewCenterX,
            translationY = target.exactCenterY() - viewCenterY
        )
    }

    private fun animateScrim(toFraction: Float) {
        scrimAnimator?.cancel()
        val from = currentScrimFraction()
        scrimAnimator = ValueAnimator.ofFloat(from, toFraction).apply {
            duration = if (toFraction > from) ENTER_DURATION else EXIT_DURATION
            interpolator = this@MediaPreview.interpolator
            addUpdateListener { setScrimFraction(it.animatedValue as Float) }
            start()
        }
    }

    private fun currentScrimFraction(): Float {
        val bg = binding.root.background ?: return 0f
        return bg.alpha / 255f
    }

    private fun setScrimFraction(fraction: Float) {
        binding.root.background?.alpha = (fraction * 255f).toInt().coerceIn(0, 255)
    }

    private fun broadcastLongPressPreview() {
        /*
         * This will broadcast out that the user long pressed to preview
         */
        val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
        localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_LONG_PRESS, true)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
    }

    private data class Transform(
        val scale: Float,
        val translationX: Float,
        val translationY: Float
    )

    companion object {
        private const val ENTER_DURATION = 240L
        private const val EXIT_DURATION = 200L
        private const val FALLBACK_START_SCALE = 0.85f
    }
}
