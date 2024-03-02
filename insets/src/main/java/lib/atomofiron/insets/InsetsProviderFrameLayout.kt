/*
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
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