package lib.atomofiron

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import lib.atomofiron.insets.InsetsProvider
import lib.atomofiron.insets.InsetsProviderImpl

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class MainLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), InsetsProvider by InsetsProviderImpl() {
    init {
        onInit(this)
    }
}