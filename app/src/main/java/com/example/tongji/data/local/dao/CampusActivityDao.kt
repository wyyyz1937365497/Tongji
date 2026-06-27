package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.CampusActivityEntity

@Dao
interface CampusActivityDao {
    @Query("SELECT * FROM campus_activities ORDER BY activityDate DESC")
    suspend fun getAll(): List<CampusActivityEntity>

    @Query("SELECT * FROM campus_activities WHERE moduleCode = :module ORDER BY activityDate DESC")
    suspend fun getByModule(module: String): List<CampusActivityEntity>

    @Query("SELECT * FROM campus_activities WHERE remoteId = :id")
    suspend fun getById(id: Long): CampusActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<CampusActivityEntity>)

    @Query("DELETE FROM campus_activities")
    suspend fun deleteAll()
}
