package com.example.tongji.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_space_rooms")
data class LibrarySpaceRoomEntity(
    @PrimaryKey
    val roomId: String,
    val libraryId: String,
    val libraryName: String,
    val floorId: String,
    val floorName: String,
    val name: String,
    val mergedName: String,
    val typeName: String,
    val syncTime: Long = System.currentTimeMillis()
)
