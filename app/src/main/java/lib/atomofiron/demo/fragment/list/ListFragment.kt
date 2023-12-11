package lib.atomofiron.demo.fragment.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import app.atomofiron.android_window_insets_compat.R
import app.atomofiron.android_window_insets_compat.databinding.FragmentListBinding
import lib.atomofiron.insets.syncInsets

class ListFragment : Fragment(R.layout.fragment_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)
        binding.apply {
            root.adapter = StringAdapter()
            root.syncInsets().padding(top = true, bottom = true)
        }
    }
}