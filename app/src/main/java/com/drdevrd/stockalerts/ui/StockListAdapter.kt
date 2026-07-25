package com.drdevrd.stockalerts.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drdevrd.stockalerts.data.AlertMode
import com.drdevrd.stockalerts.data.StockEntity
import com.drdevrd.stockalerts.databinding.ItemStockBinding

class StockListAdapter(
    private val onDelete: (StockEntity) -> Unit,
    private val onSelectionChanged: (Set<Long>) -> Unit
) : RecyclerView.Adapter<StockListAdapter.VH>() {

    private var allItems: List<StockEntity> = emptyList()
    private var filtered: MutableList<StockEntity> = mutableListOf()
    private var query: String = ""
    private val selectedIds = mutableSetOf<Long>()

    fun submit(newItems: List<StockEntity>) {
        allItems = newItems
        // Drop any selections for stocks that no longer exist (e.g. deleted elsewhere)
        selectedIds.retainAll(newItems.map { it.id }.toSet())
        applyFilter()
    }

    fun setQuery(q: String) {
        query = q
        applyFilter()
    }

    private fun applyFilter() {
        filtered = if (query.isBlank()) {
            allItems.toMutableList()
        } else {
            allItems.filter {
                it.symbol.contains(query, ignoreCase = true) ||
                it.displayName.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds)
    }

    fun selectAllVisible(select: Boolean) {
        if (select) selectedIds.addAll(filtered.map { it.id })
        else selectedIds.removeAll(filtered.map { it.id }.toSet())
        notifyDataSetChanged()
        onSelectionChanged(selectedIds)
    }

    fun getSelectedStocks(): List<StockEntity> = allItems.filter { it.id in selectedIds }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedIds)
    }

    inner class VH(val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val stock = filtered[position]
        holder.binding.symbolText.text = "${stock.symbol}  (${stock.exchange})"
        holder.binding.nameText.text = stock.displayName

        val detail = buildString {
            when (stock.alertMode) {
                AlertMode.DAILY_CLOSE -> append("Daily close alert")
                AlertMode.TARGET_CROSS -> append("Target: ${stock.targetPrice}")
                AlertMode.BOTH -> append("Daily close + target ${stock.targetPrice}")
            }
            stock.lastKnownPrice?.let { append("  •  last: %.2f".format(it)) }
        }
        holder.binding.detailText.text = detail

        // Avoid stray listener firing from view recycling
        holder.binding.selectCheckbox.setOnCheckedChangeListener(null)
        holder.binding.selectCheckbox.isChecked = stock.id in selectedIds
        holder.binding.selectCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedIds.add(stock.id) else selectedIds.remove(stock.id)
            onSelectionChanged(selectedIds)
        }

        holder.binding.deleteButton.setOnClickListener { onDelete(stock) }
    }

    override fun getItemCount() = filtered.size
}
