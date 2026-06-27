package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.GradeCourseRecordEntity
import com.example.tongji.data.local.entity.GradeSummaryEntity

@Dao
interface GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: GradeSummaryEntity)

    @Query("SELECT * FROM grade_summary WHERE id = 1")
    suspend fun getSummary(): GradeSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCourses(courses: List<GradeCourseRecordEntity>)

    @Query("SELECT * FROM grade_course_records ORDER BY sortIndex")
    suspend fun getAllCourses(): List<GradeCourseRecordEntity>

    @Query("SELECT DISTINCT termName, termCode FROM grade_course_records ORDER BY termCode DESC")
    suspend fun getTerms(): List<TermInfo>

    @Query("SELECT * FROM grade_course_records WHERE termCode = :termCode ORDER BY sortIndex")
    suspend fun getCoursesForTerm(termCode: Int): List<GradeCourseRecordEntity>

    @Query("DELETE FROM grade_summary")
    suspend fun deleteSummary()

    @Query("DELETE FROM grade_course_records")
    suspend fun deleteAllCourses()
}

data class TermInfo(
    val termName: String,
    val termCode: Int
)
