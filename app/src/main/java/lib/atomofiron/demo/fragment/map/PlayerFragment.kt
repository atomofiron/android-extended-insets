package lib.atomofiron.demo.fragment.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import app.atomofiron.android_window_insets_compat.R
import app.atomofiron.android_window_insets_compat.databinding.FragmentPlayerBinding
import lib.atomofiron.demo.fragment.list.ListFragment
import lib.atomofiron.insets.syncInsets

class PlayerFragment : Fragment(R.layout.fragment_player) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentPlayerBinding.bind(view).apply {
            searchBar.syncInsets().margin(start = true, top = true, end = true)
            btnStart.syncInsets().margin(start = true, bottom = true)
            btnEnd.syncInsets().margin(end = true, bottom = true)
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