package demo.atomofiron.insets

import android.app.Activity
import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import demo.atomofiron.insets.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import lib.atomofiron.insets.composeInsets
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.syncInsets

class DemoActivity : Activity() {

    private val cutoutDrawable = CutoutDrawable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        //lib.atomofiron.insets.debugInsets = true

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)
            //root.foreground = cutoutDrawable

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
            var systemBarsBehavior = false
            switchFullscreen.setOnClickListener { switch ->
                switch as MaterialSwitch
                insetsController.run {
                    if (switch.isChecked) hide(Type.systemBars) else show(Type.systemBars)
                }
                insetsController.systemBarsBehavior = when {
                    systemBarsBehavior -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    else -> WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                }
                if (switch.isChecked) {
                    systemBarsBehavior = !systemBarsBehavior
                }
            }
        }
    }

    private fun ActivityDemoBinding.configureInsets() {
        val topDelegate = viewTop.syncInsets(dependency = true)
            .margin(start = true, end = true)
            .padding(top = true)
        val bottomDelegate = viewBottom.syncInsets(ExtType.all, dependency = true)
            .margin(start = true, end = true)
            .padding(bottom = true)
        fab.syncInsets(ExtType.all).margin(end = true, bottom = true)
        toolbar.syncInsets(ExtType.all).margin(start = true, top = true, end = true)

        root.composeInsets(
            bottomPanel.syncInsets(dependency = true).padding(start = true, end = true, bottom = true),
        ) { _, windowInsets -> // insets modifier
            syncCutout(windowInsets)
            switchFullscreen.isChecked = windowInsets.isEmpty(Type.systemBars)
            val insets = Insets.of(0, 0, 0, bottomPanel.visibleHeightBottom)
            ExtendedWindowInsets.Builder(windowInsets)
                .setInsets(ExtType.togglePanel, insets)
                .build()
        }
        intermediate.composeInsets(topDelegate, bottomDelegate) { _, windowInsets ->
            val insets = Insets.of(0, viewTop.visibleHeight, 0, viewBottom.visibleHeightBottom)
            ExtendedWindowInsets.Builder(windowInsets)
                .setInsets(ExtType.verticalPanels, insets)
                .build()
        }
    }

    private fun syncCutout(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(Type.displayCutout)
        when {
            insets.left > 0 -> cutoutDrawable.left()
            insets.top > 0 -> cutoutDrawable.top()
            insets.right > 0 -> cutoutDrawable.right()
        }
    }
}