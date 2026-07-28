package com.drdevrd.stockalerts.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks ORDER BY exchange, symbol")
    fun observeAll(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY exchange, symbol")
    suspend fun getAll(): List<StockEntity>

    @Query("SELECT * FROM stocks WHERE exchange = :exchange")
    suspend fun getByExchange(exchange: Exchange): List<StockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stock: StockEntity): Long

    @Update
    suspend fun update(stock: StockEntity)

    @Delete
    suspend fun delete(stock: StockEntity)

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun count(): Int

    @Query("SELECT * FROM stocks WHERE symbol = :symbol AND exchange = :exchange LIMIT 1")
    suspend fun find(symbol: String, exchange: Exchange): StockEntity?
}
