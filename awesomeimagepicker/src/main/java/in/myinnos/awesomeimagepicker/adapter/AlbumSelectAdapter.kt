package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.databinding.AlbumSelectRowItemBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.MediaStoreType
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView

abstract class AlbumSelectAdapter(private val context: Context,
                                  private val albums: List<Album>) : RecyclerView.Adapter<AlbumSelectAdapter.ViewHolder>() {

    private val TAG = AlbumSelectAdapter::class.java.simpleName

    open abstract fun clicked(position: Int, album: Album)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlbumSelectRowItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val binding = holder.binding

        try {
            val album: Album = albums[position]

            var albumName = "${album.name} (${album.count})"

            binding.textView.text = albumName

            Glide.with(context)
                .load(album.uri)
                .apply(RequestOptions.placeholderOf(ColorDrawable(-0xd0d0e)))
                .apply(RequestOptions.overrideOf(200, 200))
                .apply(RequestOptions.centerCropTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageView)

            binding.rowView.setOnClickListener { clicked(position, album) }

        } catch (e: Exception) {
            Log.e(TAG, "Error in getView 2: " + e.message, e)
        }
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    inner class ViewHolder(val binding: AlbumSelectRowItemBinding) : RecyclerView.ViewHolder(binding.root)
}