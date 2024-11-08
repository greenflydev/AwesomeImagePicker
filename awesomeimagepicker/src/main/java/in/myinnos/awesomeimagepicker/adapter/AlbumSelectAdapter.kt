package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.databinding.AlbumSelectRowItemBinding
import `in`.myinnos.awesomeimagepicker.models.Album
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import `in`.myinnos.awesomeimagepicker.helpers.GalleryUtil

abstract class AlbumSelectAdapter(private val context: Context) : RecyclerView.Adapter<AlbumSelectAdapter.ViewHolder>() {

    private val TAG = AlbumSelectAdapter::class.java.simpleName

    abstract fun clicked(position: Int, album: Album)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlbumSelectRowItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val binding = holder.binding

        try {
            val album: Album = GalleryUtil.albums[position]

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
        return GalleryUtil.albums.size
    }

    inner class ViewHolder(val binding: AlbumSelectRowItemBinding) : RecyclerView.ViewHolder(binding.root)
}