package `in`.myinnos.awesomeimagepicker.views

import `in`.myinnos.awesomeimagepicker.databinding.CustomGalleryToolbarBinding
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.view.LayoutInflater

class CustomToolbar @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyle: Int = 0,
            defStyleRes: Int = 0) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    private var binding: CustomGalleryToolbarBinding =
            CustomGalleryToolbarBinding.inflate(LayoutInflater.from(context), this, true)

    fun setCallback(callback: Callback?) {
        binding.toolbarBack.setOnClickListener { callback?.onBack() }
        binding.toolbarDone.setOnClickListener { callback?.onDone() }
    }

    fun setTitle(title: String) {
        binding.toolbarTitle.text = title
    }

    fun showDone(show: Boolean) {
        binding.toolbarDone.visibility = if (show) VISIBLE else GONE
    }

    interface Callback {
        fun onBack()
        fun onDone()
    }
}