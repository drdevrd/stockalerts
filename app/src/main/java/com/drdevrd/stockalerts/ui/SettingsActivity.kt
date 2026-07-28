package com.drdevrd.stockalerts.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.drdevrd.stockalerts.alerts.AlertEngine
import com.drdevrd.stockalerts.data.Exchange
import com.drdevrd.stockalerts.data.Prefs
import com.drdevrd.stockalerts.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(com.drdevrd.stockalerts.R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.apiKeyInput.setText(Prefs.getFinnhubApiKey(this))
        binding.gsheetsUrlInput.setText(Prefs.getGoogleSheetsCsvUrl(this))
        binding.repeatIntervalInput.setText(Prefs.getRepeatIntervalMinutes(this).toString())

        binding.batteryOptButton.setOnClickListener {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }

        binding.saveButton.setOnClickListener {
            val key = binding.apiKeyInput.text?.toString()?.trim().orEmpty()
            val gsheetsUrl = binding.gsheetsUrlInput.text?.toString()?.trim().orEmpty()
            val interval = binding.repeatIntervalInput.text?.toString()?.trim()?.toIntOrNull() ?: 15
            Prefs.setFinnhubApiKey(this, key)
            Prefs.setGoogleSheetsCsvUrl(this, gsheetsUrl)
            Prefs.setRepeatIntervalMinutes(this, interval.coerceAtLeast(1))
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.testNseButton.setOnClickListener { runTest(Exchange.NSE) }
        binding.testUsButton.setOnClickListener { runTest(Exchange.US) }
    }

    private fun runTest(exchange: Exchange) {
        val button = if (exchange == Exchange.NSE) binding.testNseButton else binding.testUsButton
        button.isEnabled = false
        button.text = "Testing..."

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AlertEngine.runCheck(this@SettingsActivity, exchange, force = true)
            }
            button.isEnabled = true
            button.text = if (exchange == Exchange.NSE) "Test NSE alert now" else "Test US alert now"

            val summary = buildString {
                append("${exchange.name}: ${result.stocksInList} stocks in list\n")
                append("Fetched successfully: ${result.fetchSucceeded}\n")
                append("Failed to fetch: ${result.fetchFailed}\n")
                if (result.failedSymbols.isNotEmpty()) {
                    append("Failed symbols: ${result.failedSymbols.take(10).joinToString(", ")}")
                    if (result.failedSymbols.size > 10) append(" (+${result.failedSymbols.size - 10} more)")
                    append("\n")
                }
                result.sampleError?.let {
                    append("\nExample error:\n$it\n")
                }
                append(if (result.notificationShown) "\nNotification WAS shown - check your notification shade." else "\nNo notification shown.")
            }

            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("Test result")
                .setMessage(summary)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
