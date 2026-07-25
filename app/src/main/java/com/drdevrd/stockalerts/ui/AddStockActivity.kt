package com.drdevrd.stockalerts.ui

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(com.drdevrd.stockalerts.R.string.add_stock)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.saveButton.setOnClickListener { save() }
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
