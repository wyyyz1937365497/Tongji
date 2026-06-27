package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_summary")
data class GradeSummaryEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalGradePoint: String,
    val actualCredit: String,
    val failingCredits: String,
    val failingCourseCount: String,
    val syncTime: Long = System.currentTimeMillis()
)
