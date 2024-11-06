package `in`.myinnos.awesomeimagepicker.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_in
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_out
import `in`.myinnos.awesomeimagepicker.adapter.AlbumSelectAdapter
import `in`.myinnos.awesomeimagepicker.databinding.ActivityAlbumSelectBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.helpers.GalleryUtil
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.MediaStoreType
import `in`.myinnos.awesomeimagepicker.views.CustomToolbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlbumActivity : HelperActivity() {

    companion object {
        val albums = mutableListOf<Album>()
    }

    private lateinit var binding: ActivityAlbumSelectBinding

    private lateinit var mediaSelectResult: ActivityResultLauncher<Intent>

    private var adapter: AlbumSelectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlbumSelectBinding.inflate(layoutInflater)

        setContentView(binding.getRoot())

        val intent = intent
        if (intent == null) {
            finish()
        }

        mediaSelectResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                setResult(RESULT_OK, result.data)
                finish()
            }
        }

        ConstantsCustomGallery.limit = intent.getIntExtra(
            ConstantsCustomGallery.INTENT_EXTRA_LIMIT,
            ConstantsCustomGallery.DEFAULT_LIMIT
        )

        ConstantsCustomGallery.mediaStoreType = MediaStoreType.MIXED
        if (intent.hasExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIASTORETYPE)) {
            val mediaStoreType: MediaStoreType? = intent.getSerializableExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIASTORETYPE) as MediaStoreType?
            if (mediaStoreType != null) {
                ConstantsCustomGallery.mediaStoreType = mediaStoreType
            }
        }

        setMessageDisplays()

        binding.toolbar.setTitle(getString(R.string.album_view))

        binding.recyclerView.setHasFixedSize(false)
        val llm = LinearLayoutManager(this)
        llm.orientation = RecyclerView.VERTICAL
        binding.recyclerView.layoutManager = llm

        binding.toolbar.setCallback(object : CustomToolbar.Callback {
            override fun onBack() {
                finish()
                overridePendingTransition(abc_fade_in, abc_fade_out)
            }

            override fun onDone() {
                sendIntent()
            }
        })

        /*
         * Whenever this is loaded for the first time, clear out the list of selected items.
         * Only clear it here because the user should be able to go between albums and select
         * more items.
         */
        ConstantsCustomGallery.currentlySelectedMap.clear()

        loadAlbums()
    }

    override fun onResume() {
        super.onResume()

        /*
         * If the user changes between albums we need to show the selected count right away
         * along with the done button
         */
        displaySelectedCount()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadAlbums() {

        GlobalScope.launch(Dispatchers.Main) {

            albums.clear()
            albums.addAll(GalleryUtil.getMedia(this@AlbumActivity, ConstantsCustomGallery.mediaStoreType))

            adapter = object: AlbumSelectAdapter(this@AlbumActivity, albums) {

                override fun clicked(position: Int, album: Album) {

                    /*
                     * This will broadcast out that the user selected an album.
                     * Used for tracking in the calling application.
                     */
                    val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
                    localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_ALBUM_SELECTED, true)
                    localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, album.id)
                    LocalBroadcastManager.getInstance(this@AlbumActivity).sendBroadcast(localIntent)

                    val intent = Intent(applicationContext, MediaActivity::class.java)
                    intent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, album.id)
//                    startActivityForResult(intent, ConstantsCustomGallery.REQUEST_CODE)
                    mediaSelectResult.launch(intent)
                }
            }

            binding.recyclerView.setAdapter(adapter);

            binding.loader.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private fun displaySelectedCount() {
        val selectedCount = ConstantsCustomGallery.currentlySelectedMap.size
        if (selectedCount == 0) {
            binding.selectedCount.visibility = View.GONE
            binding.toolbar.showDone(false)
        } else {
            var itemSelected = getString(R.string.item_selected, selectedCount)
            if (selectedCount > 1) {
                itemSelected = getString(R.string.items_selected, selectedCount)
            }
            binding.selectedCount.text = itemSelected
            binding.selectedCount.visibility = View.VISIBLE
            binding.toolbar.showDone(true)
        }
    }

    private fun setMessageDisplays() {
        binding.errorDisplay.visibility = View.INVISIBLE

        val mediaTypeName = if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.VIDEOS) {
            getString(R.string.album_select_videos)
        } else if (ConstantsCustomGallery.mediaStoreType == MediaStoreType.IMAGES) {
            getString(R.string.album_select_photos)
        } else {
            getString(R.string.album_select_media)
        }

        binding.emptyView.text = getString(R.string.activity_media_empty, mediaTypeName)
    }

    private fun sendIntent() {

        ConstantsCustomGallery.getPreviouslySelectedIds(this).addAll(ConstantsCustomGallery.currentlySelectedMap.keys)
        ConstantsCustomGallery.savePreviouslySelectedIds(this)

        val selectedVideos = ArrayList<Media>()
        for ((_, value) in ConstantsCustomGallery.currentlySelectedMap) {
            selectedVideos.add(value)
        }

        val intent = Intent()
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA, selectedVideos)
        setResult(RESULT_OK, intent)
        finish()
        overridePendingTransition(abc_fade_in, abc_fade_out)
    }

}