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
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat

@Deprecated("Use ViewInsetsController")
class ViewGroupInsetsProxy private constructor() : OnApplyWindowInsetsListener {
    companion object {

        @Deprecated("Use ViewInsetsController.comsume(View)", ReplaceWith("ViewInsetsController.consume(view)"))
        fun consume(view: View) = ViewInsetsController.consume(view)

        @Deprecated("Use ViewInsetsController.setProxy(View, InsetsListener?)")
        fun set(viewGroup: View, listener: InsetsListener? = null): ViewGroupInsetsProxy {
            ViewInsetsController.setProxy(viewGroup, listener)
            return ViewGroupInsetsProxy()
        }

        @Deprecated(
            message = "Use ViewInsetsController.dispatchChildrenWindowInsets(View, WindowInsetsCompat)",
            replaceWith = ReplaceWith("ViewInsetsController.dispatchChildrenWindowInsets(viewGroup, insets)"),
        )
        fun dispatchChildrenWindowInsets(viewGroup: View, insets: WindowInsetsCompat) {
            ViewInsetsController.dispatchChildrenWindowInsets(viewGroup, insets)
        }
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) = WindowInsetsCompat.CONSUMED
}