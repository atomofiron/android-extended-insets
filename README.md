# How to use for example
[![](https://jitpack.io/v/atomofiron/android-window-insets-compat.svg)](https://jitpack.io/#atomofiron/android-window-insets-compat)

```gradle
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.atomofiron:android-window-insets-compat:1.0.0'
}
```

```kotlin
fun onViewCreated(view: View) {

    // ...

    ViewGroupInsetsProxy.set(view)
    ViewGroupInsetsProxy.set(swipeRefreshLayout)
    ViewInsetsController.bindPadding(appBar, start = true, top = true, end = true)
    ViewInsetsController.bindPadding(recyclerView, start = true, top = true, end = true, bottom = true)
    ViewInsetsController.bindPadding(bottomBar, start = true, end = true, bottom = true)
    ViewInsetsController.bindMargin(fab, end = true, bottom = true)
}
```
