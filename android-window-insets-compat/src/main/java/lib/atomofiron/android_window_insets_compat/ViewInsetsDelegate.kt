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
import androidx.core.view.WindowInsetsCompat



internal class ViewInsetsDelegate(
    private val view: View,
    private val typeMask: Int,
    private val destination: Destination,
) : ViewInsetsKeeper {

    private var insetStart = 0
    private var insetTop = 0
    private var insetEnd = 0
    private var insetBottom = 0

    private val isRtlEnabled get() = view.layoutDirection == View.LAYOUT_DIRECTION_RTL

    private val Insets.start: Int get() = if (isRtlEnabled) right else left
    private val Insets.end: Int get() = if (isRtlEnabled) left else right

    override fun updatePadding(start: Int?, top: Int?, end: Int?, bottom: Int?) {
        view.run {
            if (destination is Destination.Padding) {
                val paddingStart = (start?.let { it + insetStart } ?: view.paddingStart)
                val paddingTop = (top?.let { it + insetTop } ?: view.paddingTop)
                val paddingEnd = (end?.let { it + insetEnd } ?: view.paddingEnd)
                val paddingBottom = (bottom?.let { it + insetBottom } ?: view.paddingBottom)
                setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom)
            } else {
                setPaddingRelative(start ?: paddingStart, top ?: paddingTop, end ?: paddingEnd, bottom ?: paddingBottom)
            }
        }
    }

    override fun updateMargin(start: Int?, top: Int?, end: Int?, bottom: Int?) {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        view.layoutParams = layoutParams.apply {
            if (destination is Destination.Margin) {
                topMargin = start?.let { it + insetStart } ?: topMargin
                marginStart = top?.let { it + insetTop } ?: marginStart
                marginEnd = end?.let { it + insetEnd } ?: marginEnd
                bottomMargin = bottom?.let { it + insetBottom } ?: bottomMargin
            } else {
                topMargin = start ?: topMargin
                marginStart = top ?: marginStart
                marginEnd = end ?: marginEnd
                bottomMargin = bottom ?: bottomMargin
            }
        }
    }

    internal fun applyInsets(windowInsets: WindowInsetsCompat) {
        when (destination) {
            is Destination.Padding -> updatePadding(windowInsets)
            is Destination.Margin -> updateMargin(windowInsets)
        }
    }

    private fun updatePadding(windowInsets: WindowInsetsCompat) {
        val insets = windowInsets.getInsets(typeMask)
        var start = view.paddingStart
        if (destination.applyStart) {
            start += insets.start - insetStart
            insetStart = insets.start
        }
        var top = view.paddingTop
        if (destination.applyTop) {
            top += insets.top - insetTop
            insetTop = insets.top
        }
        var end = view.paddingEnd
        if (destination.applyEnd) {
            end += insets.end - insetEnd
            insetEnd = insets.end
        }
        var bottom = view.paddingBottom
        if (destination.applyBottom) {
            bottom += insets.bottom - insetBottom
            insetBottom = insets.bottom
        }
        val needUpdate = view.run {
            start != paddingStart || top != paddingTop || end != paddingEnd || bottom != paddingBottom
        }
        if (needUpdate) {
            view.setPaddingRelative(start, top, end, bottom)
        }
    }

    private fun updateMargin(windowInsets: WindowInsetsCompat) {
        (view.layoutParams as ViewGroup.MarginLayoutParams).run {
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
                view.layoutParams = this
            }
        }
    }
}