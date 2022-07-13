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

import android.util.LayoutDirection
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type

typealias InsetsListener = (View, WindowInsetsCompat) -> Unit

class ViewInsetsController private constructor(
    private val view: View,
    private val destination: Destination,
    private val withProxy: Boolean,
    private val typeMask: Int,
    private var listener: InsetsListener? = null,
) : OnApplyWindowInsetsListener {
    companion object {

        val defaultTypeMask = Type.systemBars() or Type.ime() or Type.displayCutout()

        fun bindPadding(
            view: View,
            start: Boolean = false,
            top: Boolean = false,
            end: Boolean = false,
            bottom: Boolean = false,
            withProxy: Boolean = false,
            typeMask: Int = defaultTypeMask,
        ): ViewInsetsController {
            require(start || top || end || bottom)
            val destination = Destination.Padding(start, top, end, bottom)
            val controller = ViewInsetsController(view, destination, withProxy, typeMask)
            ViewCompat.setOnApplyWindowInsetsListener(view, controller)
            return controller
        }

        fun bindMargin(
            view: View,
            start: Boolean = false,
            top: Boolean = false,
            end: Boolean = false,
            bottom: Boolean = false,
            withProxy: Boolean = false,
            typeMask: Int = defaultTypeMask,
        ): ViewInsetsController {
            require(start || top || end || bottom)
            val destination = Destination.Margin(start, top, end, bottom)
            val controller = ViewInsetsController(view, destination, withProxy, typeMask)
            ViewCompat.setOnApplyWindowInsetsListener(view, controller)
            return controller
        }

        fun getInsets(view: View, typeMask: Int = defaultTypeMask): Insets {
            val insets = ViewCompat.getRootWindowInsets(view) ?: WindowInsetsCompat.CONSUMED
            return insets.getInsets(typeMask)
        }

        fun consume(view: View) = ViewCompat.setOnApplyWindowInsetsListener(view) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }

        fun setProxy(viewGroup: View, listener: InsetsListener? = null): ViewInsetsController {
            viewGroup as ViewGroup
            val controller = ViewInsetsController(
                view = viewGroup,
                destination = Destination.None,
                withProxy = true,
                typeMask = defaultTypeMask,
                listener = listener,
            )
            ViewCompat.setOnApplyWindowInsetsListener(viewGroup, controller)
            return controller
        }

        fun dispatchChildrenWindowInsets(viewGroup: View, insets: WindowInsetsCompat) {
            viewGroup as ViewGroup
            val windowInsets = insets.toWindowInsets()
            for (index in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(index)
                child.dispatchApplyWindowInsets(windowInsets)
            }
        }
    }

    private sealed class Destination(
        val applyStart: Boolean,
        val applyTop: Boolean,
        val applyEnd: Boolean,
        val applyBottom: Boolean,
    ) {
        object None : Destination(false, false, false, false)
        class Padding(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean) : Destination(start, top, end, bottom)
        class Margin(start: Boolean, top: Boolean, end: Boolean, bottom: Boolean) : Destination(start, top, end, bottom)
    }

    private var insetStart = 0
    private var insetTop = 0
    private var insetEnd = 0
    private var insetBottom = 0

    private val isRtlEnabled get() = view.resources.configuration.layoutDirection == LayoutDirection.RTL

    override fun onApplyWindowInsets(view: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat {
        when (destination) {
            Destination.None -> Unit
            is Destination.Padding -> view.updatePadding(windowInsets)
            is Destination.Margin -> view.updateMargin(windowInsets)
        }
        if (withProxy) {
            dispatchChildrenWindowInsets(view, windowInsets)
        }
        listener?.invoke(view, windowInsets)
        return WindowInsetsCompat.CONSUMED
    }

    fun setListener(listener: InsetsListener) {
        this.listener = listener
    }

    fun updatePadding(start: Int? = null, top: Int? = null, end: Int? = null, bottom: Int? = null) {
        val paddingStart = (start?.let { it + insetStart } ?: view.paddingStart)
        val paddingTop = (top?.let { it + insetTop } ?: view.paddingTop)
        val paddingEnd = (end?.let { it + insetEnd } ?: view.paddingEnd)
        val paddingBottom = (bottom?.let { it + insetBottom } ?: view.paddingBottom)
        view.setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom)
    }

    private val Insets.start: Int get() = if (isRtlEnabled) right else left

    private val Insets.end: Int get() = if (isRtlEnabled) left else right

    private fun View.updatePadding(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(typeMask)
        var start = paddingStart
        if (destination.applyStart) {
            start += insets.start - insetStart
            insetStart = insets.start
        }
        var top = paddingTop
        if (destination.applyTop) {
            top += insets.top - insetTop
            insetTop = insets.top
        }
        var end = paddingEnd
        if (destination.applyEnd) {
            end += insets.end - insetEnd
            insetEnd = insets.end
        }
        var bottom = paddingBottom
        if (destination.applyBottom) {
            bottom += insets.bottom - insetBottom
            insetBottom = insets.bottom
        }
        val needUpdate = start != paddingStart || top != paddingTop || end != paddingEnd || bottom != paddingBottom
        if (needUpdate) {
            setPaddingRelative(start, top, end, bottom)
        }
    }

    private fun View.updateMargin(windowInsets: WindowInsetsCompat) {
        (layoutParams as ViewGroup.MarginLayoutParams).run {
            val insets = windowInsets.getInsets(typeMask)
            var start = marginStart
            if (destination.applyStart) {
                start += insets.start - insetStart
                insetStart = insets.start
            }
            var top = topMargin
            if (destination.applyTop) {
                top += insets.top - insetTop
                insetTop = insets.top
            }
            var end = marginEnd
            if (destination.applyEnd) {
                end += insets.end - insetEnd
                insetEnd = insets.end
            }
            var bottom = bottomMargin
            if (destination.applyBottom) {
                bottom += insets.bottom - insetBottom
                insetBottom = insets.bottom
            }
            val needUpdate = marginStart != start || topMargin != top || marginEnd != end || bottomMargin != bottom
            if (needUpdate) {
                marginStart = start
                topMargin = top
                marginEnd = end
                bottomMargin = bottom
                layoutParams = this
            }
        }
    }
}