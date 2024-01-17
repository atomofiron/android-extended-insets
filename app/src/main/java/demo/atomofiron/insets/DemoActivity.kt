package demo.atomofiron.insets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import demo.atomofiron.insets.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import demo.atomofiron.insets.fragment.map.PlayerFragment
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.ExtendedWindowInsets.Type
import lib.atomofiron.insets.composeInsets
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.withInsetsMargin
import lib.atomofiron.insets.withInsetsPadding
import lib.atomofiron.insets.withInsets

class DemoActivity : AppCompatActivity() {

    private val cutoutDrawable = CutoutDrawable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        lib.atomofiron.insets.debugInsets = true
        lib.atomofiron.insets.customTypeNameProvider = {
            when (it) {
                ExtType.togglePanel -> "togglePanel"
                ExtType.verticalPanels -> "verticalPanels"
                else -> null
            }
        }

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)
            //root.foreground = cutoutDrawable

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top, panelsContainer::requestInsets)
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom, panelsContainer::requestInsets)
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
            toolbar.setNavigationOnClickListener { }
            fab.setOnClickListener {
                supportFragmentManager.run {
                    if (fragments.isNotEmpty()) return@run
                    beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.fragments_container, PlayerFragment())
                        .commit()
                }
            }
            supportFragmentManager.addOnBackStackChangedListener {
                val show = supportFragmentManager.fragments.isEmpty()
                toolbar.isVisible = show
                if (show) fab.show() else fab.hide()
            }
        }
    }

    private fun ActivityDemoBinding.configureInsets() {
        root.composeInsets(
            bottomPanel.withInsetsPadding(dependency = true, horizontal = true, bottom = true),
        ) { _, windowInsets -> // insets modifier
            syncCutout(windowInsets)
            switchFullscreen.isChecked = windowInsets.isEmpty(Type.systemBars)
            val insets = Insets.of(0, 0, 0, bottomPanel.visibleBottomHeight)
            ExtendedWindowInsets.Builder(windowInsets)
                .setInsets(ExtType.togglePanel, insets)
                .build()
        }
        val topDelegate = viewTop.withInsets(dependency = true) { margin(horizontal).padding(top) }
        val bottomDelegate = viewBottom.withInsets(ExtType.common, dependency = true) { horizontal(margin).bottom(padding) }
        panelsContainer.composeInsets(topDelegate, bottomDelegate) { _, windowInsets ->
            val insets = Insets.of(0, viewTop.visibleTopHeight, 0, viewBottom.visibleBottomHeight)
            ExtendedWindowInsets.Builder(windowInsets)
                .setInsets(ExtType.verticalPanels, insets)
                .build()
        }
        val toolbarCombining = insetsCombining.copy(
            minStart = resources.getDimensionPixelSize(R.dimen.toolbar_navigation_padding),
            minEnd= resources.getDimensionPixelSize(R.dimen.toolbar_menu_padding),
        )
        toolbar.withInsetsMargin(ExtType.common, toolbarCombining, top = true, horizontal = true)
        val fabCombining = insetsCombining.copy(insetsCombining.combiningTypeMask or ExtType.togglePanel)
        fab.withInsetsMargin(ExtType.common, fabCombining, end = true, bottom = true)
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