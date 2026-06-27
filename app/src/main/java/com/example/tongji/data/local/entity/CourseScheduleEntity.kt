package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_schedules")
data class CourseScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseName: String,
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekNumber: Int,
    val weekText: String?,
    val semester: String,
    val syncTime: Long = System.currentTimeMillis()
)
