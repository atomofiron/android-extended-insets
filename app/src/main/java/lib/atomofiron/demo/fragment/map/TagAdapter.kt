package lib.atomofiron.demo.fragment.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import app.atomofiron.android_window_insets_compat.R
import app.atomofiron.android_window_insets_compat.databinding.ItemStringBinding

data class TagItem(val value: String)

class ItemCallbackImpl : DiffUtil.ItemCallback<TagItem>() {
    override fun areItemsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
        return oldItem == newItem
    }
}

class TagItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding = ItemStringBinding.bind(itemView)

    init {
        binding.root.setOnClickListener { }
    }

    fun bind(item: TagItem) {
        binding.tvTitle.text = item.value
    }
}

class TagAdapter : ListAdapter<TagItem, TagItemHolder>(ItemCallbackImpl()) {

    init {
        setHasStableIds(true)
        submitList((0..30).map { TagItem("Item $it") }.toMutableList())
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagItemHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_string, parent, false)
        return TagItemHolder(itemView)
    }

    override fun onBindViewHolder(holder: TagItemHolder, position: Int) {
        holder.bind(currentList[position])
    }
}