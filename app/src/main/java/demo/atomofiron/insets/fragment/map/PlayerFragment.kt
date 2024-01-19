package demo.atomofiron.insets.fragment.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import demo.atomofiron.insets.ExtType
import demo.atomofiron.insets.R
import demo.atomofiron.insets.databinding.FragmentPlayerBinding
import demo.atomofiron.insets.fragment.list.ListFragment
import lib.atomofiron.insets.insetsCombining
import lib.atomofiron.insets.withInsetsMargin

class PlayerFragment : Fragment(R.layout.fragment_player) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentPlayerBinding.bind(view).apply {
            searchBar.withInsetsMargin(ExtType.common, insetsCombining, top = true, horizontal = true)
            val combining = insetsCombining.run { copy(combiningTypeMask or ExtType.togglePanel) }
            btnStart.withInsetsMargin(ExtType.common, combining, start = true, bottom = true)
            btnEnd.withInsetsMargin(ExtType.common, combining, end = true, bottom = true)
            btnStart.setOnClickListener { showAnotherFragment(true) }
            btnEnd.setOnClickListener { showAnotherFragment(false) }
        }
    }

    private fun showAnotherFragment(direction: Boolean) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                if (direction) R.anim.transition_enter_from_start else R.anim.transition_enter_from_end,
                if (direction) R.anim.transition_exit_to_end else R.anim.transition_exit_to_start,
                if (direction) R.anim.transition_enter_from_end else R.anim.transition_enter_from_start,
                if (direction) R.anim.transition_exit_to_start else R.anim.transition_exit_to_end,
            )
            .addToBackStack(null)
            .hide(this)
            .add(R.id.fragments_container, ListFragment(), null)
            .commit()
    }
}