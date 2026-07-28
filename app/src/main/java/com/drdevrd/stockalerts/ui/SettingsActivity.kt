package com.drdevrd.stockalerts.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.drdevrd.stockalerts.data.Prefs
import com.drdevrd.stockalerts.databinding.ActivitySettingsBinding

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
            val interval = binding.repeatIntervalInput.text?.toString()?.trim()?.toIntOrNull() ?: 15
            Prefs.setFinnhubApiKey(this, key)
            Prefs.setRepeatIntervalMinutes(this, interval.coerceAtLeast(1))
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
