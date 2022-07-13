package lib.atomofiron.demo

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.view.WindowCompat
import app.atomofiron.android_window_insets_compat.R
import lib.atomofiron.android_window_insets_compat.*

class DemoActivity : Activity() {

    private lateinit var root: View
    private lateinit var viewStart: View
    private lateinit var viewTop: View
    private lateinit var viewEnd: View
    private lateinit var viewBottom: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        root = findViewById(R.id.main_root)
        viewStart = findViewById(R.id.view_start)
        viewTop = findViewById(R.id.view_top)
        viewEnd = findViewById(R.id.view_end)
        viewBottom = findViewById(R.id.view_bottom)

        val radioDestination = findViewById<RadioGroup>(R.id.radio_destination)
        radioDestination.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_padding -> applyPadding()
                R.id.radio_margin -> applyMargin()
            }
        }
        radioDestination.check(R.id.radio_padding)

        root.insetsProxying()
    }

    private fun applyPadding() {
        resetMargin()
        viewStart.applyPaddingInsets(start = true)
        viewTop.applyPaddingInsets(start = true, top = true, end = true)
        viewEnd.applyPaddingInsets(end = true)
        viewBottom.applyPaddingInsets(start = true, bottom = true, end = true)
        root.requestApplyInsets()
    }

    private fun applyMargin() {
        resetPadding()
        viewStart.applyMarginInsets(start = true)
        viewTop.applyMarginInsets(start = true, top = true, end = true)
        viewEnd.applyMarginInsets(end = true)
        viewBottom.applyMarginInsets(start = true, bottom = true, end = true)
        root.requestApplyInsets()
    }

    private fun resetPadding() {
        viewStart.setPaddingRelative(0, 0, 0, 0)
        viewTop.setPaddingRelative(0, 0, 0, 0)
        viewEnd.setPaddingRelative(0, 0, 0, 0)
        viewBottom.setPaddingRelative(0, 0, 0, 0)
    }

    private fun resetMargin() {
        viewStart.resetMargin()
        viewTop.resetMargin()
        viewEnd.resetMargin()
        viewBottom.resetMargin()
    }

    private fun View.resetMargin() {
        this.layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = 0
            topMargin = 0
            marginEnd = 0
            bottomMargin = 0
        }
    }
}