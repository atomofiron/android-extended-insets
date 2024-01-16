package demo.atomofiron.insets.fragment.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import demo.atomofiron.insets.R
import demo.atomofiron.insets.databinding.ItemStringBinding
import lib.atomofiron.insets.padding
import lib.atomofiron.insets.syncInsets

data class StringItem(val value: String)

class ItemCallbackImpl : DiffUtil.ItemCallback<StringItem>() {
    override fun areItemsTheSame(oldItem: StringItem, newItem: StringItem): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: StringItem, newItem: StringItem): Boolean {
        return oldItem == newItem
    }
}

class StringItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding = ItemStringBinding.bind(itemView)

    init {
        binding.root.setOnClickListener { }
    }

    fun bind(item: StringItem) {
        binding.tvTitle.text = item.value
    }
}

class StringAdapter : ListAdapter<StringItem, StringItemHolder>(ItemCallbackImpl()) {

    init {
        setHasStableIds(true)
        submitList((0..30).map { StringItem("Item $it") }.toMutableList())
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringItemHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_string, parent, false)
        itemView.syncInsets().padding(/* todo parent?, */start = true, end = true)
        return StringItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: StringItemHolder, position: Int) {
        holder.bind(currentList[position])
    }
}