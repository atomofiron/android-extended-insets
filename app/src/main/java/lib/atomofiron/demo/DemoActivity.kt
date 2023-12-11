package lib.atomofiron.demo

import android.app.Activity
import android.os.Bundle
import android.widget.RadioGroup
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
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

    private lateinit var startDelegate: ViewInsetsDelegate
    private lateinit var topDelegate: ViewInsetsDelegate
    private lateinit var endDelegate: ViewInsetsDelegate
    private lateinit var bottomDelegate: ViewInsetsDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)

            configureInsets()

            switchConnection.setOnCheckedChangeListener { _, isChecked ->
                viewTop.isVisible = isChecked
            }
            switchEat.setOnCheckedChangeListener { _, isChecked ->
                viewBottom.isVisible = isChecked
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
        startDelegate = viewStart.syncInsets()
        topDelegate = viewTop.syncInsets(dependency = true)
        endDelegate = viewEnd.syncInsets()
        bottomDelegate = viewBottom.syncInsets(dependency = true)

        root.composeInsets(
            // these receive original insets (from parent provider or stock system window insets)
            topDelegate,
            bottomPanel.syncInsets(dependency = true).padding(start = true, end = true, bottom = true),
        ) { _, windowInsets -> // insets modifier
            switchFullscreen.isChecked = windowInsets.isEmpty(Type.systemBars())
            val overlay = Insets.of(0, viewTop.visibleHeight, 0, bottomPanel.height)
            val insets = Insets.max(windowInsets.systemBars(), overlay)
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(Type.systemBars(), insets)
                .build()
        }
        fragmentsContainer.composeInsets(bottomDelegate) { _, windowInsets ->
            val overlay = Insets.of(0, 0, 0, viewBottom.visibleHeight)
            val insets = Insets.max(windowInsets.systemBars(), overlay)
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(Type.systemBars(), insets)
                .build()
        }
    }

    private fun ActivityDemoBinding.byPadding() {
        startDelegate.reset().padding(start = true)
        topDelegate.reset().padding(start = true, top = true, end = true)
        endDelegate.reset().padding(end = true)
        bottomDelegate.reset().padding(start = true, bottom = true, end = true)
        root.requestApplyInsets()
    }

    private fun ActivityDemoBinding.byMargin() {
        startDelegate.reset().margin(start = true)
        topDelegate.reset().margin(start = true, top = true, end = true)
        endDelegate.reset().margin(end = true)
        bottomDelegate.reset().margin(start = true, bottom = true, end = true)
        root.requestApplyInsets()
    }
}