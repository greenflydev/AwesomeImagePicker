package `in`.myinnos.awesomeimagepicker.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

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

    protected fun checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission == PackageManager.PERMISSION_GRANTED) {
                permissionGranted()
            } else {
                ActivityCompat.requestPermissions(this, permissions, ConstantsCustomGallery.PERMISSION_REQUEST_CODE)
            }
        } else {
            permissionGranted()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showRequestPermissionRationale()
            } else {
                showAppPermissionSettings()
            }
        }
    }

    private fun showRequestPermissionRationale() {
        view?.let {
            val snackbar = Snackbar.make(it, getString(R.string.permission_info), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.permission_ok)) {
                    ActivityCompat.requestPermissions(
                        this@HelperActivity,
                        permissions,
                        ConstantsCustomGallery.PERMISSION_REQUEST_CODE
                    )
                }
            snackbar.show()
        }
    }

    private fun showAppPermissionSettings() {
        view?.let {
            val snackbar = Snackbar.make(it, getString(R.string.permission_force), Snackbar.LENGTH_INDEFINITE)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != ConstantsCustomGallery.PERMISSION_REQUEST_CODE || grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            permissionDenied()
        } else {
            permissionGranted()
        }
    }

    protected open fun permissionGranted() {
    }

    private fun permissionDenied() {
        hideViews()
        requestPermission()
    }

    protected fun hideViews() {
    }

    protected fun setSnackbarView(view: View?) {
        this.view = view
    }
}
