package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_space_areas")
data class LibrarySpaceAreaEntity(
    @PrimaryKey
    val areaId: String,
    val libraryId: String,
    val libraryName: String,
    val floorId: String,
    val floorName: String,
    val name: String,
    val mergedName: String,
    val typeName: String,
    val totalSeats: Int,
    val freeSeats: Int,
    val syncTime: Long = System.currentTimeMillis()
)
