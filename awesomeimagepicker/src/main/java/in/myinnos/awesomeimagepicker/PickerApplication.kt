package `in`.myinnos.awesomeimagepicker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class PickerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}