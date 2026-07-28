package com.drdevrd.stockalerts.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drdevrd.stockalerts.data.AlertMode
import com.drdevrd.stockalerts.data.StockEntity
import com.drdevrd.stockalerts.databinding.ItemStockBinding

class StockListAdapter(
    private val onDelete: (StockEntity) -> Unit
) : RecyclerView.Adapter<StockListAdapter.VH>() {

    private val items = mutableListOf<StockEntity>()

    fun submit(newItems: List<StockEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val stock = items[position]
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

        holder.binding.deleteButton.setOnClickListener { onDelete(stock) }
    }

    override fun getItemCount() = items.size
}
