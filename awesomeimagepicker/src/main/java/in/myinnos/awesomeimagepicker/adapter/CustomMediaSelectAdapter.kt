package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.databinding.GridViewMediaSelectBinding
import `in`.myinnos.awesomeimagepicker.databinding.PreviewVideoBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.Video
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.util.*
import java.util.concurrent.TimeUnit


abstract class CustomMediaSelectAdapter(private val context: Context,
                                        private val mediaList: List<Media>) : RecyclerView.Adapter<CustomMediaSelectAdapter.ViewHolder>() {

    abstract fun clicked(position: Int)

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

    private fun previewVideo(media: Media) {

        val binding = PreviewVideoBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(context)
        builder.setView(binding.root)
        builder.setCancelable(true)

        val dialog = builder.create()

        dialog.setOnDismissListener {
            binding.videoView.stopPlayback()
        }

        binding.root.setOnClickListener {
            dialog.dismiss()
        }

        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        var originalWidth = 0
        var originalHeight = 0

        binding.videoView.setVideoURI(media.uri)
        binding.videoView.setOnPreparedListener { mediaPlayer ->

            binding.videoView.setBackgroundColor(Color.TRANSPARENT)

            originalWidth = mediaPlayer.videoWidth
            originalHeight = mediaPlayer.videoHeight

            if (originalWidth != 0 && originalHeight != 0) {

                val layoutParams = binding.videoView.layoutParams
                val orientation = context.resources.configuration.orientation

//                // Landscape or Square
//                if (originalWidth >= originalHeight) {
//                    layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
//                    layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
//                }
//                // Portrait
//                else {
//                    layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
//                    layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
//                }
//                binding.videoView.layoutParams = layoutParams
            }

            /*
                val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
                val screenRatio = videoView.width / videoView.height.toFloat()
                val scaleX = videoRatio / screenRatio
                if (scaleX >= 1f) {
                    videoView.scaleX = scaleX
                } else {
                    videoView.scaleY = 1f / scaleX
                }
             */

            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.isLooping = true;

            binding.videoView.start()
        }

        dialog.show()
    }

    private fun previewVideoOld(view: View, media: Media) {

        val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.preview_video, null)

        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        val popupWindow = PopupWindow(popupView, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view , Gravity.CENTER, 0, 0)

        val videoView = popupView.findViewById<VideoView>(R.id.video_view)

        popupView.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            videoView.stopPlayback()
        }

        var originalWidth = 0
        var originalHeight = 0

        videoView.setVideoURI(media.uri)
        videoView.setOnPreparedListener { mediaPlayer ->

            originalWidth = mediaPlayer.videoWidth
            originalHeight = mediaPlayer.videoHeight

            if (originalWidth != 0 && originalHeight != 0) {

                val layoutParams = videoView.layoutParams
                val orientation = context.resources.configuration.orientation

                // Landscape or Square
                if (originalWidth >= originalHeight) {
                    layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT
                    layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT
                }
                // Portrait
                else {
                    // Orientation doesn't matter for portrait
                    layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT
                    layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT
                }
                videoView.layoutParams = layoutParams
            }

            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.isLooping = true;

            videoView.start()
        }
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    class ViewHolder internal constructor(var binding: GridViewMediaSelectBinding) : RecyclerView.ViewHolder(binding.root)
}