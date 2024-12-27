package `in`.myinnos.awesomeimagepicker.views

import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.databinding.LongPressFtueBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * When the user opens the media picker, the first time they will see a FTUE
 */
class LongPressFtue @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyle, defStyleRes) {

    private val binding = LongPressFtueBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.done.background.colorFilter = PorterDuffColorFilter(0xFF2C7BE5.toInt(), PorterDuff.Mode.SRC_OVER)

        binding.done.setOnClickListener {
            binding.videoView.stopPlayback()
            binding.videoView.setVideoURI(null)
            binding.root.visibility = View.GONE
        }
    }

    fun checkLongPressFTUE() {
//        val viewedFtue: Boolean = ConstantsCustomGallery.getBooleanFromMainSP(context, ConstantsCustomGallery.SP_LONG_PRESS_FTUE_VIEWED)
//        if (!viewedFtue) {
            displayLongPressFTUE()
//        }
    }

    private fun displayLongPressFTUE() {

        ConstantsCustomGallery.saveBooleanToMainSP(context, ConstantsCustomGallery.SP_LONG_PRESS_FTUE_VIEWED, true)
        broadcastLongPressFTUE()

        binding.previewRoundedClip.clipToOutline = true

        // The FTUE is a video that is packaged with the app
        val path = "android.resource://${context.packageName}/${R.raw.long_press_ftue}"
        binding.videoView.setVideoURI(Uri.parse(path))

        binding.videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.isLooping = true

            binding.videoView.start();
        }

        binding.root.visibility = View.VISIBLE
    }

    private fun broadcastLongPressFTUE() {
        /*
         * This will broadcast out that the user selected saw the FTUE
         */
        val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
        localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_LONG_PRESS_FTUE, true)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
    }
}
