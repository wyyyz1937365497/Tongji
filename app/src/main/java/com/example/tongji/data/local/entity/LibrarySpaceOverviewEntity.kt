package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_space_overviews")
data class LibrarySpaceOverviewEntity(
    @PrimaryKey
    val libraryId: String,
    val name: String,
    val totalSeats: Int,
    val freeSeats: Int,
    val isTargetLibrary: Boolean,
    val syncTime: Long = System.currentTimeMillis()
)
