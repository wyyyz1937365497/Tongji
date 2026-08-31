package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.PracticeHourRecordEntity

@Dao
interface PracticeHourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PracticeHourRecordEntity>)

    @Query("SELECT * FROM practice_hour_records ORDER BY activityDate DESC")
    suspend fun getAll(): List<PracticeHourRecordEntity>

    @Query("DELETE FROM practice_hour_records")
    suspend fun deleteAll()
}
