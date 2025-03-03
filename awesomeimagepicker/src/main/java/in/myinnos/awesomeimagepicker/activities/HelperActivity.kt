package `in`.myinnos.awesomeimagepicker.activities

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import `in`.myinnos.awesomeimagepicker.R
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_in
import `in`.myinnos.awesomeimagepicker.R.anim.abc_fade_out
import `in`.myinnos.awesomeimagepicker.helpers.ConstantsCustomGallery
import `in`.myinnos.awesomeimagepicker.models.Media


/**
 * Created by MyInnos on 03-11-2016.
 */
open class HelperActivity : AppCompatActivity() {
    protected var view: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * This will force the status and navigation bar to be visible on android 15
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

            // To force full screen for testing
            //WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            val decorView = window.decorView

            ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View, insets: WindowInsetsCompat ->
                insets.toWindowInsets()?.let { it ->
                    val windowInsets = WindowInsetsCompat.toWindowInsetsCompat(it)
                    val left = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).left
                    val top = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                    val right = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right
                    val bottom = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

                    // Apply padding to prevent overlap with system bars
                    v.setPadding(left, top, right, bottom)
                }
                insets
            }
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

    fun showLimitedAccess(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED)) {
            // Full access on Android 13 (API level 33) or higher
            println("CAM DEBUG FULL ACCESS 13 or higher")
            return false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_VISUAL_USER_SELECTED) == PermissionChecker.PERMISSION_GRANTED) {
            // Partial access on Android 14 (API level 34) or higher
            println("CAM DEBUG PARTIAL ACCESS 14 or higher")
            return true
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            println("CAM DEBUG FULL ACCESS up to 12")
            return false
        } else {
            // Access denied
            println("CAM DEBUG ACCESS DENIED")
            return true
        }
    }

    private fun showAppPermissionSettings(messageId: Int) {
        view?.let {
            val snackbar = Snackbar.make(it, getString(messageId), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.permission_settings)) {
                    val uri = Uri.fromParts(
                        getString(R.string.permission_package),
                        this@HelperActivity.packageName,
                        null
                    )
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.setData(uri)
                    startActivityForResult(intent, ConstantsCustomGallery.PERMISSION_REQUEST_CODE)
                }
            snackbar.show()
        }
    }

    protected open fun permissionGranted() {
    }

    protected fun setSnackbarView(view: View?) {
        this.view = view
    }
}
