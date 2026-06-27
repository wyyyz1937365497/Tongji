package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teaching_notices")
data class TeachingNoticeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val publishTimeText: String,
    val topStatus: Int,
    val read: Boolean = false,
    val syncTime: Long = System.currentTimeMillis()
)
