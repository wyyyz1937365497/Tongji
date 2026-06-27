package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.CourseScheduleEntity

@Dao
interface CourseScheduleDao {
    @Query("SELECT * FROM course_schedules WHERE weekNumber = :weekNumber ORDER BY dayOfWeek, startPeriod")
    suspend fun getSchedulesForWeek(weekNumber: Int): List<CourseScheduleEntity>

    @Query("SELECT * FROM course_schedules ORDER BY dayOfWeek, startPeriod")
    suspend fun getAllSchedules(): List<CourseScheduleEntity>

    @Query("SELECT DISTINCT semester FROM course_schedules")
    suspend fun getSemesters(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<CourseScheduleEntity>)

    @Query("DELETE FROM course_schedules")
    suspend fun deleteAll()
}
