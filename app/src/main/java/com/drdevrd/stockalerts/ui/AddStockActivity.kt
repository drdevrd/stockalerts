package com.drdevrd.stockalerts.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.drdevrd.stockalerts.data.AlertMode
import com.drdevrd.stockalerts.data.AppDatabase
import com.drdevrd.stockalerts.data.Exchange
import com.drdevrd.stockalerts.data.StockEntity
import com.drdevrd.stockalerts.databinding.ActivityAddStockBinding
import kotlinx.coroutines.launch

class AddStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStockBinding
    private var editingStockId: Long = -1L

    companion object {
        const val EXTRA_STOCK_ID = "stock_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        editingStockId = intent.getLongExtra(EXTRA_STOCK_ID, -1L)

        if (editingStockId != -1L) {
            supportActionBar?.title = "Edit Stock"
            loadExistingStock(editingStockId)
        } else {
            supportActionBar?.title = getString(com.drdevrd.stockalerts.R.string.add_stock)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.saveButton.setOnClickListener { save() }
    }

    private fun loadExistingStock(id: Long) {
        lifecycleScope.launch {
            val stock = AppDatabase.getInstance(this@AddStockActivity).stockDao().getById(id) ?: return@launch

            binding.symbolInput.setText(stock.symbol)
            binding.nameInput.setText(stock.displayName)
            binding.targetInput.setText(stock.targetPrice?.toString().orEmpty())

            if (stock.exchange == Exchange.NSE) binding.radioNse.isChecked = true else binding.radioUs.isChecked = true
            // Symbol and exchange identify which stock this is - lock them in edit mode
            // so this screen only ever changes target price and alert mode, never
            // silently creates a second entry for a different symbol/exchange.
            binding.symbolInput.isEnabled = false
            binding.radioNse.isEnabled = false
            binding.radioUs.isEnabled = false

            when (stock.alertMode) {
                AlertMode.DAILY_CLOSE -> binding.modeDailyClose.isChecked = true
                AlertMode.TARGET_CROSS -> binding.modeTargetOnly.isChecked = true
                AlertMode.BOTH -> binding.modeBoth.isChecked = true
            }
        }
    }

    private fun save() {
        val symbol = binding.symbolInput.text?.toString()?.trim()?.uppercase().orEmpty()
        if (symbol.isEmpty()) {
            Toast.makeText(this, "Enter a symbol", Toast.LENGTH_SHORT).show()
            return
        }

        val exchange = if (binding.radioNse.isChecked) Exchange.NSE else Exchange.US
        val name = binding.nameInput.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: symbol
        val targetText = binding.targetInput.text?.toString()?.trim()
        val target = targetText?.toDoubleOrNull()

        val alertMode = when {
            binding.modeTargetOnly.isChecked -> AlertMode.TARGET_CROSS
            binding.modeBoth.isChecked -> AlertMode.BOTH
            else -> AlertMode.DAILY_CLOSE
        }

        if ((alertMode == AlertMode.TARGET_CROSS || alertMode == AlertMode.BOTH) && target == null) {
            Toast.makeText(this, "Enter a target price for that alert mode", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val dao = AppDatabase.getInstance(this@AddStockActivity).stockDao()

            if (editingStockId != -1L) {
                val existing = dao.getById(editingStockId)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            displayName = name,
                            targetPrice = target,
                            alertMode = alertMode,
                            // A newly set/changed target should be eligible to fire again,
                            // not blocked by a previous day's already-hit flag.
                            targetAlreadyHit = false
                        )
                    )
                }
                finish()
                return@launch
            }

            val existing = dao.find(symbol, exchange)
            if (existing != null) {
                Toast.makeText(this@AddStockActivity, "Already tracking $symbol", Toast.LENGTH_SHORT).show()
                return@launch
            }
            dao.insert(
                StockEntity(
                    symbol = symbol,
                    exchange = exchange,
                    displayName = name,
                    targetPrice = target,
                    alertMode = alertMode
                )
            )
            finish()
        }
    }
}
