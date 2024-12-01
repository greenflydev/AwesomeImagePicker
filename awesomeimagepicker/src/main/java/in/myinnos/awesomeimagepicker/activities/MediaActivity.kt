package `in`.myinnos.awesomeimagepicker.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_in
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_out
import `in`.myinnos.awesomeimagepicker.adapter.MediaSelectAdapter
import `in`.myinnos.awesomeimagepicker.databinding.ActivityImageSelectBinding
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.helpers.GalleryUtil
import `in`.myinnos.awesomeimagepicker.models.Album
import `in`.myinnos.awesomeimagepicker.models.Media
import `in`.myinnos.awesomeimagepicker.models.MediaType
import `in`.myinnos.awesomeimagepicker.views.CustomToolbar

class MediaActivity : HelperActivity() {

    private val TAG = MediaActivity::class.java.simpleName

    private lateinit var binding: ActivityImageSelectBinding

    private lateinit var album: Album

    private var currentMediaType = MediaType.MIXED

    private var adapter: MediaSelectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageSelectBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val intent = intent
        if (intent == null) {
            finish()
        }
        val albumId = intent.getLongExtra(ConstantsCustomGallery.INTENT_EXTRA_ALBUM_ID, 0)
        GalleryUtil.albums.find { it.id == albumId }?.let {
            album = it
        }
        if (!::album.isInitialized) {
            Log.e(TAG, "Error no album found for $albumId")
            finish()
            return
        }

        /*
         * This is the media type that comes from GF. Can be mixed, photos, or videos.
         *
         * Using a local variable so when the user filters to only photos or videos we
         * wont override the global type.
         */
        currentMediaType = ConstantsCustomGallery.mediaType

        var titleId = R.string.media_view
        when (currentMediaType) {
            MediaType.VIDEOS -> titleId = if (ConstantsCustomGallery.limit == 1) {
                R.string.single_video_view
            } else {
                R.string.video_view
            }

            MediaType.IMAGES -> titleId = if (ConstantsCustomGallery.limit == 1) {
                R.string.single_image_view
            } else {
                R.string.image_view
            }

            else -> {}
        }
        binding.toolbar.setTitle(getString(titleId, ConstantsCustomGallery.limit))

        binding.toolbar.setCallback(object : CustomToolbar.Callback {
            override fun onBack() {
                finish()
                overridePendingTransition(abc_fade_in, abc_fade_out)
            }

            override fun onDone() {
                sendIntent()
            }
        })

        setupTabLayout()

        /*
         * If the user changes between albums we need to show the selected count right away
         * along with the done button
         */
        displaySelectedCount()

        binding.longPressFtue.checkLongPressFTUE()

        adapter = object : MediaSelectAdapter(this@MediaActivity, album) {
            override fun clicked(media: Media) {
                toggleSelection(media)

                displaySelectedCount()
            }

            override fun longClicked(media: Media) {
                showMediaPreview(media)
            }
        }
        binding.recyclerView.adapter = adapter

        when (album.mediaList.size) {
            0 -> {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }
            else -> {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
        orientationBasedUI(resources.configuration.orientation)
    }

    private fun orientationBasedUI(orientation: Int) {
        val windowManager = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val spanCount = if ((orientation == Configuration.ORIENTATION_PORTRAIT)) 3 else 5

        binding.recyclerView.setHasFixedSize(false)

        val gridLayoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerView.layoutManager = gridLayoutManager
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> {
                return false
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orientationBasedUI(newConfig.orientation)
    }

    private fun showMediaPreview(media: Media) {
        /*
         * This will check if the user should see the FTUE, and if they should
         * then display it to the user.
         */
        binding.mediaPreview.showMediaPreview(media)
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

    /*
     * Sets up the tabs along the top, if the mode is mixed media.
     * The user can pick to see all, just photos, or just videos.
     */
    private fun setupTabLayout() {

        if (ConstantsCustomGallery.mediaType == MediaType.MIXED) {

            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_all_media)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_photos)));
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_videos)));
            binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL;

            binding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        tabSelected(it.position)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }
            })
        } else {
            binding.tabLayout.visibility = View.GONE
        }
    }

    /*
     * This will reload the media with the type of media selected
     */
    private fun tabSelected(position: Int) {

        currentMediaType = when (position) {
            ConstantsCustomGallery.TAB_PHOTOS_POSITION -> MediaType.IMAGES
            ConstantsCustomGallery.TAB_VIDEOS_POSITION -> MediaType.VIDEOS
            else -> MediaType.MIXED
        }

        adapter?.filterMedia(currentMediaType)

        /*
         * This will broadcast out that the user filtered to a media type.
         * Used for tracking in the calling application.
         */
        val localIntent = Intent(ConstantsCustomGallery.BROADCAST_EVENT)
        localIntent.putExtra(ConstantsCustomGallery.BROADCAST_EVENT_FILTER_BY_TYPE, true)
        localIntent.putExtra(ConstantsCustomGallery.INTENT_EXTRA_FILTER_BY_TYPE, position)
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)
    }

    private fun toggleSelection(media: Media) {

        val countSelected = ConstantsCustomGallery.currentlySelectedMap.size
        if (!media.isSelected && countSelected >= ConstantsCustomGallery.limit) {
            var messageId = R.string.media_limit_exceeded
            when (ConstantsCustomGallery.mediaType) {
                MediaType.VIDEOS -> messageId = R.string.video_limit_exceeded
                MediaType.IMAGES -> messageId = R.string.image_limit_exceeded
                else -> {}
            }
            Toast.makeText(applicationContext, getString(messageId, ConstantsCustomGallery.limit), Toast.LENGTH_SHORT).show()
            return
        }

        media.isSelected = !media.isSelected
        if (media.isSelected) {
            ConstantsCustomGallery.currentlySelectedMap[media.id.toString()] = media
        } else {
            ConstantsCustomGallery.currentlySelectedMap.remove(media.id.toString())
        }
        adapter?.notifyDataSetChanged()
    }
}