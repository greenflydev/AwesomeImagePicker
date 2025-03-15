package `in`.myinnos.awesomeimagepicker.activities

import android.Manifest.permission
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_in
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_out
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media


/**
 * Created by MyInnos on 03-11-2016.
 */
open class HelperActivity : AppCompatActivity() {
    protected var view: View? = null

    enum class StorageStatus {
        FULL_ACCESS,
        LIMITED_ACCESS,
        DENIED_ACCESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge(
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            )

        super.onCreate(savedInstanceState)

        applyWindowInsetsDecorView()
    }

    private fun applyWindowInsetsDecorView() {

        val decorView = window.decorView

        ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View, insets: WindowInsetsCompat ->
            insets.toWindowInsets()?.let { it ->
                val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(it)
                val left = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).left
                val top = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                val right = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right
                val bottom = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

                v.setBackgroundColor(Color.WHITE)

                // Apply padding to prevent overlap with system bars
                v.setPadding(left, top, right, bottom)
            }
            insets
        }
    }

    protected fun sendIntent() {

        ConstantsCustomGallery.getPreviouslySelectedIds(this).addAll(ConstantsCustomGallery.currentlySelectedMap.keys)
        ConstantsCustomGallery.savePreviouslySelectedIds(this)

        val selectedMedia = ArrayList<Media>()
        for ((_, value) in ConstantsCustomGallery.currentlySelectedMap) {
            selectedMedia.add(value)
        }

        val intent = Intent()
        intent.putParcelableArrayListExtra(ConstantsCustomGallery.INTENT_EXTRA_MEDIA, selectedMedia)
        setResult(RESULT_OK, intent)
        finish()
        overridePendingTransition(abc_fade_in, abc_fade_out)
    }

    fun getStorageStatus(): StorageStatus {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED)) {
            // Full access on Android 13 (API level 33) or higher
            return StorageStatus.FULL_ACCESS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_VISUAL_USER_SELECTED) == PermissionChecker.PERMISSION_GRANTED) {
            // Partial access on Android 14 (API level 34) or higher
            return StorageStatus.LIMITED_ACCESS
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            return StorageStatus.FULL_ACCESS
        } else {
            // Access denied
            return StorageStatus.DENIED_ACCESS
        }
    }
}
