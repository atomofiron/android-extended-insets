package demo.atomofiron.insets.fragment.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import demo.atomofiron.insets.ExtType
import demo.atomofiron.insets.R
import demo.atomofiron.insets.databinding.FragmentListBinding
import lib.atomofiron.insets.insetsPadding

class ListFragment : Fragment(R.layout.fragment_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)
        binding.apply {
            root.adapter = StringAdapter()
            root.insetsPadding(ExtType.common, vertical = true)
        }
    }
}