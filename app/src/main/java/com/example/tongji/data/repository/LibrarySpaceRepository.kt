package com.example.tongji.data.repository

import com.example.tongji.data.local.dao.LibrarySpaceDao
import com.example.tongji.data.local.entity.*
import com.example.tongji.data.remote.api.LibrarySpaceApi

class LibrarySpaceRepository(
    private val api: LibrarySpaceApi,
    private val dao: LibrarySpaceDao
) {
    suspend fun sync(): Result<Unit> = runCatching {
        val resp = api.quickSelect()
        val body = resp.body() ?: return@runCatching

        val libraries = body["libraries"] as? List<Map<String, Any>> ?: return@runCatching
        val targetIds = setOf("siping", "dewen", "dongqu", "jiading")

        val overviews = libraries.map { lib ->
            LibrarySpaceOverviewEntity(
                libraryId = lib["libraryId"] as? String ?: "",
                name = lib["name"] as? String ?: "",
                totalSeats = (lib["totalSeats"] as? Number)?.toInt() ?: 0,
                freeSeats = (lib["freeSeats"] as? Number)?.toInt() ?: 0,
                isTargetLibrary = (lib["libraryId"] as? String) in targetIds
            )
        }
        dao.deleteAllOverviews()
        dao.insertOverviews(overviews)

        val allAreas = mutableListOf<LibrarySpaceAreaEntity>()
        val allRooms = mutableListOf<LibrarySpaceRoomEntity>()

        for (lib in libraries) {
            val libId = lib["libraryId"] as? String ?: continue
            val areas = lib["areas"] as? List<Map<String, Any>> ?: emptyList()
            allAreas.addAll(areas.map { area ->
                LibrarySpaceAreaEntity(
                    areaId = area["areaId"] as? String ?: "",
                    libraryId = libId,
                    libraryName = lib["name"] as? String ?: "",
                    floorId = area["floorId"] as? String ?: "",
                    floorName = area["floorName"] as? String ?: "",
                    name = area["name"] as? String ?: "",
                    mergedName = area["mergedName"] as? String ?: "",
                    typeName = area["typeName"] as? String ?: "",
                    totalSeats = (area["totalSeats"] as? Number)?.toInt() ?: 0,
                    freeSeats = (area["freeSeats"] as? Number)?.toInt() ?: 0
                )
            })
            val rooms = lib["rooms"] as? List<Map<String, Any>> ?: emptyList()
            allRooms.addAll(rooms.map { room ->
                LibrarySpaceRoomEntity(
                    roomId = room["roomId"] as? String ?: "",
                    libraryId = libId,
                    libraryName = lib["name"] as? String ?: "",
                    floorId = room["floorId"] as? String ?: "",
                    floorName = room["floorName"] as? String ?: "",
                    name = room["name"] as? String ?: "",
                    mergedName = room["mergedName"] as? String ?: "",
                    typeName = room["typeName"] as? String ?: ""
                )
            })
        }
        dao.deleteAllAreas()
        dao.insertAreas(allAreas)
        dao.deleteAllRooms()
        dao.insertRooms(allRooms)
    }

    suspend fun getTargetLibraries() = dao.getTargetLibraries()
    suspend fun getOtherLibraries() = dao.getOtherLibraries()
    suspend fun getAreasForLibrary(libraryId: String) = dao.getAreasForLibrary(libraryId)
    suspend fun getRoomsForLibrary(libraryId: String) = dao.getRoomsForLibrary(libraryId)
}
