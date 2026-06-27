package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.CampusCardBalanceEntity
import com.example.tongji.data.local.entity.CampusCardTransactionEntity

@Dao
interface CampusCardDao {
    @Query("SELECT * FROM campus_card_balances ORDER BY capturedAt DESC LIMIT 1")
    suspend fun getLatestBalance(): CampusCardBalanceEntity?

    @Query("SELECT * FROM campus_card_balances ORDER BY capturedAt DESC")
    suspend fun getAllBalances(): List<CampusCardBalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: CampusCardBalanceEntity)

    @Query("SELECT * FROM campus_card_transactions ORDER BY transactionDateTime DESC LIMIT :limit")
    suspend fun getRecentTransactions(limit: Int = 50): List<CampusCardTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CampusCardTransactionEntity>)

    @Query("DELETE FROM campus_card_balances")
    suspend fun deleteAllBalances()

    @Query("DELETE FROM campus_card_transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM campus_card_transactions")
    suspend fun getTransactionCount(): Int
}
