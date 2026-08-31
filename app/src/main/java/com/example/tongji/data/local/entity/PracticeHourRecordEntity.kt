package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_hour_records")
data class PracticeHourRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calendarId: String,
    val plateId: Int,
    val plateName: String,
    val name: String,
    val nature: String,
    val hour: Double,
    val activityDate: String
)
