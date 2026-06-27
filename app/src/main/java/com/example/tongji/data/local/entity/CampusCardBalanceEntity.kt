package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campus_card_balances")
data class CampusCardBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val capturedAt: Long,
    val balanceYuan: Double,
    val account: String,
    val ownerName: String,
    val cardIdentifier: String
)
