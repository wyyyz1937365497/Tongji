package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.ExamScheduleItemEntity

@Dao
interface ExamScheduleDao {
    @Query("SELECT * FROM exam_schedule_items ORDER BY sortIndex")
    suspend fun getAll(): List<ExamScheduleItemEntity>

    @Query("SELECT * FROM exam_schedule_items WHERE examDateText IS NOT NULL AND examDateText != '' ORDER BY examDateText, startTimeText")
    suspend fun getScheduledExams(): List<ExamScheduleItemEntity>

    @Query("SELECT * FROM exam_schedule_items WHERE examDateText IS NULL OR examDateText = '' ORDER BY sortIndex")
    suspend fun getUnscheduledExams(): List<ExamScheduleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamScheduleItemEntity>)

    @Query("DELETE FROM exam_schedule_items")
    suspend fun deleteAll()
}
