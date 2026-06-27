package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_schedule_items")
data class ExamScheduleItemEntity(
    @PrimaryKey
    val sourceId: Int,
    val calendarId: Int,
    val calendarName: String,
    val switchRemark: String,
    val syncTime: Long = System.currentTimeMillis(),
    val sortIndex: Int,
    val courseName: String,
    val newCourseCode: String,
    val newTeachingClassCode: String,
    val examDateText: String?,
    val startTimeText: String?,
    val endTimeText: String?,
    val examSite: String?,
    val examTimeText: String?,
    val remark: String?,
    val examSituation: Int,
    val isOpen: Int
)
