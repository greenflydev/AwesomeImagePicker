package `in`.myinnos.awesomeimagepicker.activities

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import `in`.myinnos.awesomeimagepicker.models.MediaType
import `in`.myinnos.awesomeimagepicker.views.CustomToolbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AlbumActivity : HelperActivity() {

    private lateinit var binding: ActivityAlbumSelectBinding

    private lateinit var mediaSelectResult: ActivityResultLauncher<Intent>

    private var adapter: AlbumSelectAdapter? = null

    private val STORAGE_PERMISSION_REQUEST_CODE = 1234

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val intent = intent
        finish()
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlbumSelectBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSnackbarView(binding.root) // For handling permissions

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

        ConstantsCustomGallery.mediaType = MediaType.MIXED
        if (intent.hasExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIATYPE)) {
            val mediaType: MediaType? = intent.getSerializableExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIATYPE) as MediaType?
            if (mediaType != null) {
                ConstantsCustomGallery.mediaType = mediaType
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

        adapter = object: AlbumSelectAdapter(this@AlbumActivity) {

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
                mediaSelectResult.launch(intent)
            }
        }
        binding.recyclerView.setAdapter(adapter);

        checkPermissions()
        loadAlbums()
    }

    private fun checkPermissions() {

        if (showLimitedAccess()) {
            binding.limitedAccess.visibility = View.VISIBLE
            binding.settings.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)) //, READ_MEDIA_VISUAL_USER_SELECTED))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
                } else {
                    requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
                }
            }
        } else {
            binding.limitedAccess.visibility = View.GONE
        }
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
//            checkPermissions()
//            loadAlbums()
//        }
//    }

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

        binding.loader.visibility = View.VISIBLE
        binding.errorDisplay.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        GlobalScope.launch(Dispatchers.Main) {

            val success = GalleryUtil.loadMedia(this@AlbumActivity, ConstantsCustomGallery.mediaType)

            when (success) {
                true -> {
                    binding.loader.visibility = View.GONE
                    binding.errorDisplay.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
                false -> {
                    binding.loader.visibility = View.GONE
                    binding.errorDisplay.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }
            }
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

        val mediaTypeName = if (ConstantsCustomGallery.mediaType == MediaType.VIDEOS) {
            getString(R.string.album_select_videos)
        } else if (ConstantsCustomGallery.mediaType == MediaType.IMAGES) {
            getString(R.string.album_select_photos)
        } else {
            getString(R.string.album_select_media)
        }

        binding.emptyView.text = getString(R.string.activity_media_empty, mediaTypeName)
    }
}