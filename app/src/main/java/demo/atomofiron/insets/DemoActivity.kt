package demo.atomofiron.insets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import demo.atomofiron.insets.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import demo.atomofiron.insets.fragment.map.PlayerFragment
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.ExtendedWindowInsets.Type.Companion.invoke
import lib.atomofiron.insets.InsetsProvider
import lib.atomofiron.insets.InsetsProviderImpl
import lib.atomofiron.insets.InsetsSource
import lib.atomofiron.insets.builder
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.insetsMix
import lib.atomofiron.insets.insetsPadding
import lib.atomofiron.insets.setContentView
import lib.atomofiron.insets.setInsetsDebug

class DemoActivity : AppCompatActivity(), InsetsProvider by InsetsProviderImpl() {

    private val cutoutDrawable = CutoutDrawable()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setInsetsDebug(false)

        val root = setContentView(R.layout.activity_demo, insetsProvider = this@DemoActivity)
        val binding = ActivityDemoBinding.bind(root)
        binding.run {
            //root.foreground = cutoutDrawable

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top) { publishInsetsFrom(viewTop) }
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom) { publishInsetsFrom(viewBottom) }
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
                    Type.systemBars().let { if (switch.isChecked) hide(it) else show(it) }
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
            toolbar.setOnMenuItemClickListener {
                val currentNight = resources.getBoolean(R.bool.night)
                AppCompatDelegate.setDefaultNightMode(if (currentNight) MODE_NIGHT_NO else MODE_NIGHT_YES)
                recreate()
                true
            }
            fab.setOnClickListener {
                supportFragmentManager.takeIf { it.fragments.isEmpty() }?.run {
                    beginTransaction()
                        .addToBackStack(null)
                        .setCustomAnimations(
                            R.anim.transition_scale_fade_enter,
                            R.anim.transition_scale_fade_exit,
                            R.anim.transition_scale_fade_pop_enter,
                            R.anim.transition_scale_fade_pop_exit,
                        )
                        .replace(R.id.fragments_container, PlayerFragment())
                        .commit()
                }
            }
            fab.setOnLongClickListener {
                snackbar = Snackbar.make(snackbarContainer, "Orientation-dependent snackbar", Snackbar.LENGTH_INDEFINITE).apply { show() }
                true
            }
            supportFragmentManager.addOnBackStackChangedListener {
                val show = supportFragmentManager.fragments.isEmpty()
                toolbar.isVisible = show
                if (show) fab.show() else {
                    fab.hide()
                    snackbar?.dismiss()
                }
            }
        }
    }

    private fun ActivityDemoBinding.configureInsets() {
        setInsetsModifier { _, windowInsets ->
            syncCutout(windowInsets)
            switchFullscreen.isChecked = windowInsets.isEmpty(ExtType.systemBars)
            windowInsets.builder()
                .consume(windowInsets { ime })
                .build()
        }
        root.insetsPadding(ExtType.ime, bottom = true)
        bottomPanel.insetsPadding(horizontal = true, bottom = true)
            .source(vertical = true) {
                val insets = Insets.of(0, 0, 0, bottomPanel.visibleBottomHeight)
                InsetsSource
                    .publish(ExtType.general, insets)
                    .publish(ExtType.togglePanel, insets)
            }
        viewTop.insetsMix { margin(horizontal).padding(top) }
            .source(vertical = true) { (view, _) ->
                val insets = Insets.of(0, view.visibleTopHeight, 0, 0)
                InsetsSource
                    .publish(ExtType.general, insets)
                    .publish(ExtType.verticalPanels, insets)
            }
        viewBottom.insetsMix(ExtType { barsWithCutout + togglePanel }) { horizontal(margin).bottom(padding) }
            .source(vertical = true) {
                val insets = Insets.of(0, 0, 0, viewBottom.visibleBottomHeight)
                InsetsSource
                    .publish(ExtType.general, insets)
                    .publish(ExtType.verticalPanels, insets)
            }
        toolbar.insetsMix(ExtType { barsWithCutout + verticalPanels }) {
            top(translation).horizontal(margin)
        }
        val fabTypes = ExtType { barsWithCutout + togglePanel + verticalPanels }
        val fabCombining = insetsCombining.run { copy(combiningTypes + ExtType.togglePanel) }
        fab.insetsMix(fabTypes, fabCombining) {
            translation(bottom, end)
        }.source { (view, _) ->
            InsetsSource
                .publish(ExtType.fabTop, Insets.of(0, 0, 0, view.visibleBottomHeight))
                .publish(ExtType.fabHorizontal, Insets.of(view.visibleLeftWidth, 0, view.visibleRightWidth, 0))
        }
        // nested container with applied insets
        snackbarParentContainer.insetsMix(fabTypes) { padding(horizontal).translation(bottom) }
        snackbarParentContainer.setInsetsModifier { _, windowInsets ->
            val landscape = snackbarParentContainer.run { width > height }
            windowInsets.builder()
                // snackbar dynamic relative position
                .consume(if (landscape) ExtType.fabTop else ExtType.fabHorizontal)
                .consume(ExtType { fabTop + fabHorizontal }, windowInsets[fabTypes])
                .build()
        }
        snackbarContainer.insetsMix(ExtType { fabTop + fabHorizontal }) { translation(bottom).padding(end) }
    }

    private fun syncCutout(windowInsets: ExtendedWindowInsets) {
        val insets = windowInsets[ExtType.displayCutout]
        when {
            insets.left > 0 -> cutoutDrawable.left()
            insets.top > 0 -> cutoutDrawable.top()
            insets.right > 0 -> cutoutDrawable.right()
        }
    }
}