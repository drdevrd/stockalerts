package com.drdevrd.stockalerts.ui

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.drdevrd.stockalerts.alerts.AlarmScheduler
import com.drdevrd.stockalerts.alerts.NotificationHelper
import com.drdevrd.stockalerts.data.AlertState
import com.drdevrd.stockalerts.data.AppDatabase
import com.drdevrd.stockalerts.data.DefaultWatchlist
import com.drdevrd.stockalerts.data.Exchange
import com.drdevrd.stockalerts.data.Prefs
import com.drdevrd.stockalerts.data.StockEntity
import com.drdevrd.stockalerts.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StockListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        NotificationHelper.ensureChannel(this)

        adapter = StockListAdapter(
            onDelete = { stock -> deleteStock(stock) },
            onSelectionChanged = { selected -> updateSelectionBar(selected.size) },
            onEdit = { stock ->
                startActivity(
                    Intent(this, AddStockActivity::class.java)
                        .putExtra(AddStockActivity.EXTRA_STOCK_ID, stock.id)
                )
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.setQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAllVisible(isChecked)
        }

        binding.deleteSelectedButton.setOnClickListener { confirmDeleteSelected() }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddStockActivity::class.java))
        }

        requestRuntimePermissions()

        lifecycleScope.launch {
            seedDefaultsIfNeeded()
            observeStocks()
        }

        AlarmScheduler.scheduleAll(this)
    }

    override fun onResume() {
        super.onResume()
        for (id in listOf(NotificationHelper.NOTIF_ID_NSE, NotificationHelper.NOTIF_ID_US)) {
            if (AlertState.isActive(this, id)) {
                AlertState.clear(this, id)
                AlarmScheduler.cancelRepeat(this, id)
                NotificationHelper.cancel(this, id)
            }
        }
        refreshList()
    }

    private fun updateSelectionBar(count: Int) {
        binding.selectionBar.visibility = if (count > 0) android.view.View.VISIBLE else android.view.View.GONE
        binding.selectionCountText.text = "$count selected"
    }

    private fun confirmDeleteSelected() {
        val selected = adapter.getSelectedStocks()
        if (selected.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Delete ${selected.size} stock(s)?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getInstance(this@MainActivity).stockDao().deleteByIds(selected.map { it.id })
                    adapter.clearSelection()
                    binding.selectAllCheckbox.isChecked = false
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("Delete ALL stocks?")
            .setMessage("This removes your entire watchlist, including the default list. This can't be undone.")
            .setPositiveButton("Delete all") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getInstance(this@MainActivity).stockDao().deleteAll()
                    adapter.clearSelection()
                    binding.selectAllCheckbox.isChecked = false
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeStocks() {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MainActivity).stockDao().observeAll().collect { list ->
                adapter.submit(list)
                binding.emptyText.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val list = AppDatabase.getInstance(this@MainActivity).stockDao().getAll()
            adapter.submit(list)
            binding.emptyText.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private suspend fun seedDefaultsIfNeeded() {
        if (Prefs.isFirstLaunchDone(this)) return
        val dao = AppDatabase.getInstance(this).stockDao()
        if (dao.count() > 0) {
            Prefs.setFirstLaunchDone(this)
            return
        }
        DefaultWatchlist.NSE_DEFAULT.forEach { (symbol, name) ->
            dao.insert(StockEntity(symbol = symbol, exchange = Exchange.NSE, displayName = name, isDefault = true))
        }
        DefaultWatchlist.US_DEFAULT.forEach { (symbol, name) ->
            dao.insert(StockEntity(symbol = symbol, exchange = Exchange.US, displayName = name, isDefault = true))
        }
        Prefs.setFirstLaunchDone(this)
    }

    private fun deleteStock(stock: StockEntity) {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@MainActivity).stockDao().delete(stock)
            refreshList()
        }
    }

    private fun requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        val powerManager = getSystemService(android.os.PowerManager::class.java)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) {
                // Some OEM builds block this intent - user can whitelist manually via Settings screen.
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(com.drdevrd.stockalerts.R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            com.drdevrd.stockalerts.R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            com.drdevrd.stockalerts.R.id.action_delete_all -> {
                confirmDeleteAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
