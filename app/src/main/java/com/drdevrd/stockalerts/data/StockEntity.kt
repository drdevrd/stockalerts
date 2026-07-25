package com.drdevrd.stockalerts.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Exchange { NSE, US }

enum class AlertMode { DAILY_CLOSE, TARGET_CROSS, BOTH }

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,          // e.g. "RELIANCE" or "AAPL"
    val exchange: Exchange,
    val displayName: String,
    val targetPrice: Double? = null,
    val alertMode: AlertMode = AlertMode.DAILY_CLOSE,
    val isDefault: Boolean = false,
    val lastAlertedDate: String? = null,   // yyyy-MM-dd, prevents duplicate daily alerts
    val lastKnownPrice: Double? = null,
    val targetAlreadyHit: Boolean = false  // reset daily so target alerts don't repeat forever
)
