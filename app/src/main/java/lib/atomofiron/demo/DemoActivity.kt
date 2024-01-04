package lib.atomofiron.demo

import android.app.Activity
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import app.atomofiron.android_window_insets_compat.R
import app.atomofiron.android_window_insets_compat.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import lib.atomofiron.insets.ViewInsetsDelegate
import lib.atomofiron.insets.composeInsets
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.syncInsets
import lib.atomofiron.insets.systemBars

class DemoActivity : Activity() {

    private var systemBarsBehavior = false
    private val cutout = CutoutDrawable()

    private lateinit var topDelegate: ViewInsetsDelegate
    private lateinit var bottomDelegate: ViewInsetsDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)
            //root.foreground = cutout

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top, intermediate::requestInsets)
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom, intermediate::requestInsets)
            switchConnection.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) topCtrl.show() else topCtrl.hide()
            }
            switchEat.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) bottomCtrl.show() else bottomCtrl.hide()
            }
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            switchFullscreen.setOnClickListener { switch ->
                switch as MaterialSwitch
                insetsController.run {
                    if (switch.isChecked) hide(Type.systemBars()) else show(Type.systemBars())
                }
                insetsController.systemBarsBehavior = when {
                    systemBarsBehavior -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    else -> WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
                if (switch.isChecked) {
                    systemBarsBehavior = !systemBarsBehavior
                }
            }
            radioDestination.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_padding -> byPadding()
                    R.id.radio_margin -> byMargin()
                }
            }
            radioDestination.check(R.id.radio_padding)
        }
    }

    private fun ActivityDemoBinding.configureInsets() {
        topDelegate = viewTop.syncInsets(dependency = true)
        bottomDelegate = viewBottom.syncInsets(dependency = true)
        fab.syncInsets().margin(end = true, bottom = true)
        toolbar.syncInsets().margin(start = true, top = true, end = true)

        root.composeInsets(
            bottomPanel.syncInsets(dependency = true).padding(start = true, end = true, bottom = true),
        ) { _, windowInsets -> // insets modifier
            syncCutout(windowInsets)
            switchFullscreen.isChecked = windowInsets.isEmpty(Type.systemBars())
            val overlay = Insets.of(0, 0, 0, bottomPanel.visibleHeightBottom)
            val insets = Insets.max(windowInsets.systemBars(), overlay)
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(Type.systemBars(), insets)
                .build()
        }
        intermediate.composeInsets(topDelegate, bottomDelegate) { _, windowInsets ->
            val overlay = Insets.of(0, viewTop.visibleHeight, 0, viewBottom.visibleHeightBottom)
            val insets = Insets.max(windowInsets.systemBars(), overlay)
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(Type.systemBars(), insets)
                .build()
        }
    }

    private fun byPadding() {
        topDelegate.reset().padding(start = true, top = true, end = true)
        bottomDelegate.reset().padding(start = true, bottom = true, end = true)
    }

    private fun byMargin() {
        topDelegate.reset().margin(start = true, top = true, end = true)
        bottomDelegate.reset().margin(start = true, bottom = true, end = true)
    }

    private fun syncCutout(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(Type.displayCutout())
        when {
            insets.left > 0 -> cutout.left()
            insets.top > 0 -> cutout.top()
            insets.right > 0 -> cutout.right()
        }
    }
}