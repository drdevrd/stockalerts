package com.drdevrd.stockalerts.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromExchange(v: Exchange): String = v.name
    @TypeConverter
    fun toExchange(v: String): Exchange = Exchange.valueOf(v)

    @TypeConverter
    fun fromAlertMode(v: AlertMode): String = v.name
    @TypeConverter
    fun toAlertMode(v: String): AlertMode = AlertMode.valueOf(v)
}

@Database(entities = [StockEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stock_alerts.db"
                ).build().also { INSTANCE = it }
            }
    }
}
