/*
 * Copyright 2022 Yaroslav Nesterov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.atomofiron.android_window_insets_compat

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type



typealias InsetsListener = (View, WindowInsetsCompat) -> Unit

interface ViewInsetsKeeper {
    fun updatePadding(start: Int? = null, top: Int? = null, end: Int? = null, bottom: Int? = null)
    fun updateMargin(start: Int? = null, top: Int? = null, end: Int? = null, bottom: Int? = null)
}

val defaultTypeMask = Type.systemBars() or Type.ime() or Type.displayCutout()

fun View.insetsProxying(
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
) {
    applyInsets(destination = null, withProxying = true, typeMask, listener)
}

fun View.getInsets(typeMask: Int = defaultTypeMask): Insets {
    val windowInsets = ViewCompat.getRootWindowInsets(this) ?: WindowInsetsCompat.CONSUMED
    return windowInsets.getInsets(typeMask)
}

fun View.consumeInsets(listener: InsetsListener? = null) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        listener?.invoke(view, windowInsets)
        WindowInsetsCompat.CONSUMED
    }
}

fun View.applyPaddingInsets(
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
) = applyPaddingInsets(horizontal = true, vertical = true, withProxying, typeMask, listener)

fun View.applyPaddingInsets(
    horizontal: Boolean = false,
    vertical: Boolean = false,
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
) = applyPaddingInsets(horizontal, vertical, horizontal, vertical, withProxying, typeMask, listener)

fun View.applyPaddingInsets(
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
): ViewInsetsKeeper {
    val destination = Destination.Padding(start, top, end, bottom)
    return applyInsets(destination, withProxying, typeMask, listener)!!
}

fun View.applyMarginInsets(
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
) = applyMarginInsets(horizontal = true, vertical = true, withProxying, typeMask, listener)

fun View.applyMarginInsets(
    horizontal: Boolean = false,
    vertical: Boolean = false,
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
) = applyMarginInsets(horizontal, vertical, horizontal, vertical, withProxying, typeMask, listener)

fun View.applyMarginInsets(
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
): ViewInsetsKeeper {
    val destination = Destination.Margin(start, top, end, bottom)
    return applyInsets(destination, withProxying, typeMask, listener)!!
}

private fun View.applyInsets(
    destination: Destination?,
    withProxying: Boolean = false,
    typeMask: Int = defaultTypeMask,
    listener: InsetsListener? = null,
): ViewInsetsKeeper? {
    require(!withProxying || this is ViewGroup)
    require(destination != null || withProxying)
    require(destination == null || destination.isNotEmpty)

    val delegate = destination?.let { dst ->
        ViewInsetsDelegate(this, typeMask, dst)
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        delegate?.applyInsets(windowInsets)
        if (withProxying) {
            (this as ViewGroup).dispatchChildrenWindowInsets(windowInsets)
        }
        listener?.invoke(view, windowInsets)
        WindowInsetsCompat.CONSUMED
    }
    return delegate
}

fun ViewGroup.dispatchChildrenWindowInsets(insets: WindowInsetsCompat) {
    val windowInsets = insets.toWindowInsets()
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        child.dispatchApplyWindowInsets(windowInsets)
    }
}
