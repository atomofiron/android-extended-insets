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
import lib.atomofiron.insets.InsetsCombining
import lib.atomofiron.insets.InsetsModifier
import lib.atomofiron.insets.builder
import lib.atomofiron.insets.isEmpty
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.insetsMix
import lib.atomofiron.insets.insetsPadding
import lib.atomofiron.insets.requestInsetsOnLayoutChange
import lib.atomofiron.insets.requestInsetsOnVisibilityChange

class DemoActivity : AppCompatActivity() {

    private val cutoutDrawable = CutoutDrawable()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        lib.atomofiron.insets.setInsetsDebug(false)

        ActivityDemoBinding.inflate(layoutInflater).apply {
            setContentView(root)
            //root.foreground = cutoutDrawable

            configureInsets()

            val topCtrl = ViewTranslationAnimator(viewTop, Gravity.Top, root::requestInsets)
            val bottomCtrl = ViewTranslationAnimator(viewBottom, Gravity.Bottom, root::requestInsets)
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
                supportFragmentManager.run {
                    if (fragments.isNotEmpty()) return@run
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
        root.setInsetsModifier { _, windowInsets ->
            syncCutout(windowInsets)
            switchFullscreen.isChecked = windowInsets.isEmpty(ExtType.systemBars)
            windowInsets
        }
        root.insetsPadding(ExtType.ime, bottom = true)
            .dependency(vertical = true) {
                InsetsModifier.consume(ExtType.ime)
            }
        bottomPanel.insetsPadding(horizontal = true, bottom = true)
            .dependency(vertical = true) {
                val insets = Insets.of(0, 0, 0, bottomPanel.visibleBottomHeight)
                InsetsModifier
                    .max(ExtType.general, insets)
                    .set(ExtType.togglePanel, insets)
            }
        viewTop.insetsMix { margin(horizontal).padding(top) }
            .dependency(vertical = true) {
                val insets = Insets.of(0, it.visibleTopHeight, 0, 0)
                InsetsModifier
                    .max(ExtType.general, insets)
                    .max(ExtType.verticalPanels, insets)
            }
        viewBottom.insetsMix(ExtType { barsWithCutout + togglePanel }) { horizontal(margin).bottom(padding) }
            .dependency(vertical = true) {
                val insets = Insets.of(0, 0, 0, viewBottom.visibleBottomHeight)
                InsetsModifier
                    .max(ExtType.general, insets)
                    .max(ExtType.verticalPanels, insets)
            }
        toolbar.insetsMix(ExtType { barsWithCutout + verticalPanels }) {
            top(translation).horizontal(margin)
        }
        val fabTypes = ExtType { barsWithCutout + togglePanel + verticalPanels }
        val fabCombining = insetsCombining.run { copy(combiningTypes + ExtType.togglePanel) }
        fab.insetsMix(fabTypes, fabCombining) {
            translation(bottom, end)
        }.dependency {
            InsetsModifier
                .set(ExtType.fabTop, Insets.of(0, 0, 0, it.visibleBottomHeight))
                .set(ExtType.fabHorizontal, Insets.of(it.visibleLeftWidth, 0, it.visibleRightWidth, 0))
        }
        // todo fix
        requestInsetsOnVisibilityChange(fab)

        val spcCombining = InsetsCombining(ExtType.togglePanel, minBottom = resources.getDimensionPixelSize(R.dimen.common_padding))
        // nested container with applied insets
        val spcDelegate = snackbarParentContainer.insetsMix(fabTypes) { padding(horizontal).translation(bottom) }
        // snackbar dynamic relative position
        snackbarParentContainer.setInsetsModifier { _, windowInsets ->
            val landscape = snackbarParentContainer.run { width > height }
            spcDelegate.combining(spcCombining.takeIf { landscape })
            windowInsets.builder()
                .consume(if (landscape) ExtType.fabTop else ExtType.fabHorizontal)
                .build()
        }
        // on orientation changed
        requestInsetsOnLayoutChange(snackbarParentContainer)
        // child of nested container with decreased fab insets by consuming()
        snackbarContainer.insetsMix(ExtType { fabTop + fabHorizontal }) { translation(bottom).padding(end) }
            .consuming(fabTypes)
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