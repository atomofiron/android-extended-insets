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
import androidx.core.view.WindowInsetsCompat

@Deprecated("Use extensions")
class ViewInsetsController private constructor() : InsetsListener {
    companion object {

        @Deprecated("Use defaultTypeMask")
        val defaultTypeMask = lib.atomofiron.android_window_insets_compat.defaultTypeMask

        @Deprecated("Use View.applyPaddingInsets(Int, Boolean, InsetsListener?)")
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
            val controller = ViewInsetsController()
            val keeper = view.applyPaddingInsets(start, top, end, bottom, withProxy, typeMask, controller)
            controller.viewInsetsKeeper = keeper
            return controller
        }

        @Deprecated("Use View.applyMarginInsets(Int, Boolean, Boolean, Boolean, Boolean, Boolean, InsetsListener?)")
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
            val controller = ViewInsetsController()
            val keeper = view.applyMarginInsets(start, top, end, bottom, withProxy, typeMask, controller)
            controller.viewInsetsKeeper = keeper
            return controller
        }

        @Deprecated("Use View.getInsets(Int)", ReplaceWith("view.getInsets(typeMask)"))
        fun getInsets(view: View, typeMask: Int = defaultTypeMask) = view.getInsets(typeMask)
    }

    private lateinit var viewInsetsKeeper: ViewInsetsKeeper
    private var listener: InsetsListener? = null

    fun setListener(listener: InsetsListener) {
        this.listener = listener
    }

    fun updatePadding(start: Int? = null, top: Int? = null, end: Int? = null, bottom: Int? = null) {
        viewInsetsKeeper.updatePadding(start, top, end, bottom)
    }

    override fun invoke(view: View, windowInsets: WindowInsetsCompat) {
        listener?.invoke(view, windowInsets)
    }
}