package `in`.myinnos.awesomeimagepicker.adapter

import `in`.myinnos.awesomeimagepicker.R
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
                                  private val albums: List<Album>,
                                  private val mediaStoreType: MediaStoreType) : RecyclerView.Adapter<AlbumSelectAdapter.ViewHolder>() {

    private val TAG = AlbumSelectAdapter::class.java.simpleName

    open abstract fun clicked(position: Int, album: Album)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.album_select_row_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        try {
            val album: Album = albums[position]

            var albumName = "${album.name} (${album.count})"

            holder.textView.text = albumName

            Glide.with(context)
                .load(album.uri)
                .apply(RequestOptions.placeholderOf(ColorDrawable(-0xd0d0e)))
                .apply(RequestOptions.overrideOf(200, 200))
                .apply(RequestOptions.centerCropTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageView)

            holder.rowView.setOnClickListener { clicked(position, album) }

        } catch (e: Exception) {
            Log.e(TAG, "Error in getView 2: " + e.message, e)
        }
    }

    override fun getItemCount(): Int {
        return when (mediaStoreType) {
            MediaStoreType.MIXED -> albums.size + 2 // Photos and videos at the top
            else -> albums.size + 1 // Only photos or videos
        }
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val rowView = v.findViewById<RelativeLayout>(R.id.rowView)
        val imageView = v.findViewById<ShapeableImageView>(R.id.image_view_album_image)
        val textView = v.findViewById<TextView>(R.id.text_view_album_name)
    }
}