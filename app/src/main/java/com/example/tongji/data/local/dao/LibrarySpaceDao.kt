package com.example.tongji.data.local.dao

import androidx.room.*
import com.example.tongji.data.local.entity.*

@Dao
interface LibrarySpaceDao {
    @Query("SELECT * FROM library_space_overviews ORDER BY isTargetLibrary DESC, name")
    suspend fun getAllOverviews(): List<LibrarySpaceOverviewEntity>

    @Query("SELECT * FROM library_space_overviews WHERE isTargetLibrary = 1")
    suspend fun getTargetLibraries(): List<LibrarySpaceOverviewEntity>

    @Query("SELECT * FROM library_space_overviews WHERE isTargetLibrary = 0")
    suspend fun getOtherLibraries(): List<LibrarySpaceOverviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverviews(overviews: List<LibrarySpaceOverviewEntity>)

    @Query("DELETE FROM library_space_overviews")
    suspend fun deleteAllOverviews()

    @Query("SELECT * FROM library_space_areas WHERE libraryId = :libraryId ORDER BY floorName, name")
    suspend fun getAreasForLibrary(libraryId: String): List<LibrarySpaceAreaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<LibrarySpaceAreaEntity>)

    @Query("DELETE FROM library_space_areas")
    suspend fun deleteAllAreas()

    @Query("SELECT * FROM library_space_rooms WHERE libraryId = :libraryId ORDER BY name")
    suspend fun getRoomsForLibrary(libraryId: String): List<LibrarySpaceRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<LibrarySpaceRoomEntity>)

    @Query("DELETE FROM library_space_rooms")
    suspend fun deleteAllRooms()
}
