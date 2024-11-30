# Demo
https://github.com/atomofiron/android-window-insets-compat/assets/14147217/2369db1d-566e-4b4d-9b90-9fae9e97c2d9

# Step 1
```xml
<!-- values/bools.xml -->
<bool name="day">true</bool>
<!-- values-night/bools.xml -->
<bool name="day">false</bool>
<!-- values/themes.xml -->
<style name="AppTheme" parent="...">
    <item name="android:fitsSystemWindows">false</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:navigationBarDividerColor" tools:targetApi="27">@android:color/transparent</item>
    <item name="android:windowLightStatusBar" tools:targetApi="23">@bool/day</item>
    <item name="android:windowLightNavigationBar" tools:targetApi="27">@bool/day</item>
    <item name="android:enforceNavigationBarContrast" tools:targetApi="29">false</item>
    <item name="android:windowLayoutInDisplayCutoutMode" tools:targetApi="27">shortEdges</item>
</style>
```
# Step 2
```gradle
dependencies {
    implementation("io.github.atomofiron:extended-insets:2.0.0")
}
```
# Step 3
```kotlin
class YourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_your)
    }
}
```
```xml
<!-- activity_your.xml -->
<lib.atomofiron.insets.InsetsProviderFrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    >
<!-- ... -->
</lib.atomofiron.insets.InsetsProviderFrameLayout>
```
# Step 3 alternative 1
```kotlin
class YourActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = setContentView(R.layout.activity_your, insetsProvider = InsetsProviderImpl())
        val binding = ActivityYourBinding.bind(root) // if needed, or root.findViewById()
    }
}
```
# Step 3 alternative 2
```kotlin
// if you need the Activity to be an insets provider to have access to it not only from UI
class YourActivity : AppCompatActivity(), InsetsProvider by InsetsProviderImpl() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = setContentView(R.layout.activity_your, insetsProvider = this)
        val binding = ActivityYourBinding.bind(root) // if needed, or root.findViewById()
    }
}
```
# How to use
```kotlin
import androidx.core.graphics.Insets
import lib.atomofiron.insets.ExtendedWindowInsets
import lib.atomofiron.insets.TypeSet

// apply bottom ime insets as padding
view1.insetsPadding(Type.ime, bottom = true)
// apply insets as padding at the top and as margin horizontally 
view2.insetsMix { // Type.barsWithCutout by default
    margin(horizontal).padding(top)
}
insetsProvider.setInsetsModifier { _, windowInsets ->
    windowInsets.builder()
        // modify insets
        .build()
}
// define your oun custom inset types
object ExtType : ExtendedWindowInsets.Type() {
    val togglePanel = define("togglePanel")
    val verticalPanels = define("verticalPanels")
    val fab = define("fab")
    val general = define("general")
}
// associate your custom type with ExtendedWindowInsets
operator fun ExtendedWindowInsets.invoke(block: ExtType.() -> TypeSet): Insets = get(ExtType.block())
// provide custom insets
fab.insetsSource { view ->
    val insets = Insets.of(view.visibleLeftWidth, 0, view.visibleRightWidth, view.visibleBottomHeight)
    InsetsSource.submit(ExtType.fab, insets)
}
```
# Example
```kotlin
fun onViewCreated(view: View) {
    val binding = ViewBinding.bind(view)
    binding.configureInsets()
}
private fun ViewBinding.configureInsets() {
    root.setInsetsModifier { _, windowInsets ->
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
}
```
