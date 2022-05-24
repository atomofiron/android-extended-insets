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
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

typealias InsetsListener = (View, WindowInsetsCompat) -> Unit

class ViewGroupInsetsProxy private constructor(
    private val listener: InsetsListener?,
) : OnApplyWindowInsetsListener {
    companion object {

        fun consume(view: View) = ViewCompat.setOnApplyWindowInsetsListener(view) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }

        fun set(viewGroup: View, listener: InsetsListener? = null): ViewGroupInsetsProxy {
            viewGroup as ViewGroup
            val proxy = ViewGroupInsetsProxy(listener)
            ViewCompat.setOnApplyWindowInsetsListener(viewGroup, proxy)
            return proxy
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

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        dispatchChildrenWindowInsets(view, insets)
        listener?.invoke(view, insets)
        return WindowInsetsCompat.CONSUMED
    }
}