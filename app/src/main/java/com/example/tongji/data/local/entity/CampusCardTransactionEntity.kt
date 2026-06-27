package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campus_card_transactions")
data class CampusCardTransactionEntity(
    @PrimaryKey
    val orderId: String,
    val transactionDateTime: Long,
    val amountYuan: Double,
    val balanceYuan: Double,
    val transactionDescription: String,
    val turnoverType: String,
    val locationName: String,
    val payName: String,
    val updatedAt: Long
)
