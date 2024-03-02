/*
 * Copyright 2024 Yaroslav Nesterov
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

package lib.atomofiron.insets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class InsetsProviderFrameLayout private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int,
    insetsProvider: InsetsProvider,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), InsetsProvider by insetsProvider {

    constructor(
        context: Context,
        insetsProvider: InsetsProvider,
    ) : this(context, null, 0, 0, insetsProvider)

    constructor(
        context: Context,
        attrs: AttributeSet,
    ) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : this(context, attrs, defStyleAttr, defStyleRes, InsetsProviderImpl(context, attrs))

    init {
        onInit()
    }
}