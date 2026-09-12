package com.notishuttle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.notishuttle.databinding.ItemAppBinding

class AppListAdapter(
    private val apps: List<AppItem>,
    initiallySelected: Set<String>,
    private val onToggle: (packageName: String, selected: Boolean) -> Unit,
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val selected = initiallySelected.toMutableSet()

    class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = apps[position]
        with(holder.binding) {
            tvLabel.text = item.label
            tvPackage.text = item.packageName
            ivIcon.setImageDrawable(item.icon)

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.packageName in selected
            checkbox.setOnCheckedChangeListener { _, checked ->
                if (checked) selected.add(item.packageName) else selected.remove(item.packageName)
                onToggle(item.packageName, checked)
            }
            root.setOnClickListener { checkbox.toggle() }
        }
    }

    override fun getItemCount(): Int = apps.size
}
