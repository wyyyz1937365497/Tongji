package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campus_activities")
data class CampusActivityEntity(
    @PrimaryKey
    val remoteId: Long,
    val title: String,
    val source: String,
    val activityDate: Long?,
    val activityEndDate: Long?,
    val location: String?,
    val link: String?,
    val descriptionText: String?,
    val progressValue: Int?,
    val progressName: String?,
    val moduleCode: String?,
    val moduleName: String?,
    val starPoints: Double,
    val syncTime: Long = System.currentTimeMillis()
)
