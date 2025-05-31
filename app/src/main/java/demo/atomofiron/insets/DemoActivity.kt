package demo.atomofiron.insets

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import demo.atomofiron.insets.databinding.ActivityDemoBinding
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import demo.atomofiron.insets.fragment.map.PlayerFragment
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.InsetsProvider
import lib.atomofiron.insets.InsetsProviderImpl
import lib.atomofiron.insets.InsetsSource
import lib.atomofiron.insets.builder
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.insetsMix
import lib.atomofiron.insets.insetsPadding
import lib.atomofiron.insets.insetsSource
import lib.atomofiron.insets.insetsTranslation
import lib.atomofiron.insets.reallyDisableFitsSystemWindows
import lib.atomofiron.insets.setContentView
import lib.atomofiron.insets.setInsetsDebug
import kotlin.math.max

class DemoActivity : AppCompatActivity(), InsetsProvider by InsetsProviderImpl() {

    private val cutoutDrawable = CutoutDrawable()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // needed in some projects
        window.reallyDisableFitsSystemWindows()
        setInsetsDebug(false)

        val root = setContentView(R.layout.activity_demo, insetsProvider = this@DemoActivity)
        val binding = ActivityDemoBinding.bind(root)
        binding.run {
            //root.foreground = cutoutDrawable

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top)
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom)
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
            cutoutDrawable.sync(windowInsets)
            val fullscreen = windowInsets.isEmpty(ExtType.systemBars)
            switchFullscreen.isChecked = fullscreen
            val cutout = windowInsets { displayCutout }.run {
                val horizontal = if (fullscreen) 0 else max(left, right)
                Insets.of(horizontal, top, horizontal, bottom)
            }
            windowInsets.builder()
                .set(ExtType.general, windowInsets { barsWithCutout })
                .consume(windowInsets { ime })
                .set(ExtType.displayCutout, cutout)
                .build()
        }
        root.insetsPadding(ExtType.ime, bottom = true)
        bottomPanel.insetsPadding(horizontal = true, bottom = true)
        bottomPanel.insetsSource(vertical = true) {
            val insets = Insets.of(0, 0, 0, bottomPanel.visibleBottomHeight)
            InsetsSource
                .submit(ExtType.general, insets)
                .submit(ExtType.togglePanel, insets)
        }
        viewTop.insetsMix {
            margin(horizontal).padding(top)
        }
        viewTop.insetsSource(vertical = true) { view ->
            val insets = Insets.of(0, view.visibleTopHeight, 0, 0)
            InsetsSource
                .submit(ExtType.general, insets)
                .submit(ExtType.verticalPanels, insets)
        }
        viewBottom.insetsMix(ExtType { barsWithCutout + togglePanel }) {
            horizontal(margin).bottom(padding)
        }
        viewBottom.insetsSource(vertical = true) {
            val insets = Insets.of(0, 0, 0, viewBottom.visibleBottomHeight)
            InsetsSource
                .submit(ExtType.general, insets)
                .submit(ExtType.verticalPanels, insets)
        }
        toolbar.insetsMix(ExtType { barsWithCutout + verticalPanels }) {
            top(translation).horizontal(margin)
        }
        val fabTypes = ExtType { barsWithCutout + togglePanel + verticalPanels }
        val fabCombining = insetsCombining.run { copy(combiningTypes + ExtType.togglePanel) }
        fab.insetsTranslation(fabTypes, fabCombining, end = true, bottom = true)
        fab.insetsSource { view ->
            val insets = Insets.of(view.visibleLeftWidth, 0, view.visibleRightWidth, view.visibleBottomHeight)
            InsetsSource.submit(ExtType.fab, insets)
        }
        // nested container with applied insets
        snackbarParentContainer.insetsMix(fabTypes) {
            padding(horizontal).translation(bottom)
        }
        snackbarParentContainer.setInsetsModifier { _, windowInsets ->
            windowInsets.builder()
                .consume(ExtType.fab, windowInsets[fabTypes])
                .build()
        }
        val snackbarDelegate = snackbarContainer.insetsMix(ExtType.fab) {
            val landscape = snackbarParentContainer.run { width > height }
            // snackbar dynamic relative position
            if (landscape) padding(end) else translation(bottom)
        }
        snackbarParentContainer.addLayoutSizeChangeListener { _, _ -> snackbarDelegate.updateInsets() }
    }

    private fun CutoutDrawable.sync(windowInsets: ExtendedWindowInsets) {
        val insets = windowInsets[ExtType.displayCutout]
        when {
            insets.left > 0 -> left()
            insets.top > 0 -> top()
            insets.right > 0 -> right()
        }
    }
}