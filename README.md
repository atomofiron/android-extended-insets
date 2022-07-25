[![](https://jitpack.io/v/atomofiron/android-window-insets-compat.svg)](https://jitpack.io/#atomofiron/android-window-insets-compat)

# Demo
<img src="https://github.com/Atomofiron/android-window-insets-compat/blob/main/stuff/insets_port.png" data-canonical-src="https://github.com/Atomofiron/android-window-insets-compat/blob/main/stuff/insets_port.png" width="320" /><img src="https://github.com/Atomofiron/android-window-insets-compat/blob/main/stuff/insets_land.png" data-canonical-src="https://github.com/Atomofiron/android-window-insets-compat/blob/main/stuff/insets_land.png" width="640" />

# How to use for example
```gradle
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.atomofiron:android-window-insets-compat:1.1.0'
}
```

```kotlin
fun onViewCreated(view: View) {

    // ...

    view as ViewGroup
    view.insetsProxying()
    swipeRefreshLayout.insetsProxying()
    appBar.applyPaddingInsets(start = true, top = true, end = true)
    recyclerView.applyPaddingInsets()
    bottomBar.applyPaddingInsets(horizontal = true, withProxying = true)
    fab.applyPaddingInsets(end = true, bottom = true)
}
```
