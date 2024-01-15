package lib.atomofiron.insets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class InsetsProviderFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), InsetsProvider by InsetsProviderImpl() {
    init {
        onInit()
    }
}