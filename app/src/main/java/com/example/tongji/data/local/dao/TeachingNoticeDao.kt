package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.TeachingNoticeEntity

@Dao
interface TeachingNoticeDao {
    @Query("SELECT * FROM teaching_notices ORDER BY topStatus DESC, publishTimeText DESC")
    suspend fun getAll(): List<TeachingNoticeEntity>

    @Query("SELECT * FROM teaching_notices WHERE id = :id")
    suspend fun getById(id: String): TeachingNoticeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notices: List<TeachingNoticeEntity>)

    @Query("UPDATE teaching_notices SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM teaching_notices")
    suspend fun deleteAll()
}
