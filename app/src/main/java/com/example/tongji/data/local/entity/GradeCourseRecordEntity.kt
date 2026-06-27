package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_course_records")
data class GradeCourseRecordEntity(
    @PrimaryKey
    val sourceId: Int,
    val termCode: Int,
    val termName: String,
    val calName: String,
    val termAveragePoint: String,
    val sortIndex: Int,
    val courseCode: String,
    val newCourseCode: String,
    val courseName: String,
    val courseCategory: String?,
    val creditText: String,
    val gradePointText: String?,
    val scoreText: String?,
    val scoreExamType: String?,
    val publicCourseName: String?,
    val isPassName: String?,
    val updateTimeText: String?,
    val syncTime: Long = System.currentTimeMillis()
)
