[![](https://jitpack.io/v/atomofiron/android-window-insets-compat.svg)](https://jitpack.io/#atomofiron/android-window-insets-compat)

# Demo
https://github.com/atomofiron/android-window-insets-compat/assets/14147217/2369db1d-566e-4b4d-9b90-9fae9e97c2d9

# Step 1
```xml
<style name="AppTheme" parent="...">
    <item name="android:fitsSystemWindows">false</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:navigationBarDividerColor" tools:targetApi="o_mr1">@android:color/transparent</item>
    <item name="android:windowLightStatusBar" tools:targetApi="m">@bool/light_bars</item>
    <item name="android:windowLightNavigationBar" tools:targetApi="o_mr1">@bool/light_bars</item>
    <item name="android:enforceNavigationBarContrast" tools:targetApi="q">false</item>
    <item name="android:windowLayoutInDisplayCutoutMode" tools:targetApi="o_mr1">shortEdges</item>
</style>
```
# Step 2
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    WindowCompat.setDecorFitsSystemWindows(window, false)
}
```

# How to use
```gradle
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.atomofiron:android-window-insets-compat:1.1.1'
}
```

```kotlin
fun onViewCreated(view: View) {
    view.insets...
}
```
