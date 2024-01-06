package demo.atomofiron.insets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import lib.atomofiron.insets.InsetsProvider
import lib.atomofiron.insets.InsetsProviderImpl

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class InsetsProviderFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), InsetsProvider by InsetsProviderImpl() {
    init {
        onInit()
    }
}