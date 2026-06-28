package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.data.local.dao.LibrarySpaceDao
import com.example.tongji.data.local.entity.*
import com.example.tongji.data.remote.api.LibrarySpaceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "LibSpaceRepo"

data class AreaInfo(
    val areaId: String,
    val name: String,
    val floorName: String,
    val libraryName: String,
    val mergedName: String,
    val totalSeats: Int,
    val freeSeats: Int,
    val typeName: String
)

data class SeatInfo(
    val id: String,
    val no: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val status: String,
    val statusName: String
)

class LibrarySpaceRepository(
    private val api: LibrarySpaceApi,
    private val dao: LibrarySpaceDao
) {
    private val imageClient = OkHttpClient()
    suspend fun fetchJwt(casToken: String): String? {
        val resp = api.casUser(mapOf("cas" to casToken))
        val body = resp.body() ?: return null
        @Suppress("UNCHECKED_CAST")
        val member = body["member"] as? Map<String, Any> ?: return null
        return member["token"] as? String
    }

    suspend fun sync(): Result<Unit> = runCatching {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val resp = api.quickSelect(mapOf(
            "id" to "1",
            "date" to today,
            "categoryIds" to listOf("1"),
            "members" to 0
        ))
        val body = resp.body() ?: return@runCatching

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any> ?: return@runCatching
        @Suppress("UNCHECKED_CAST")
        val premises = data["premises"] as? List<Map<String, Any>> ?: return@runCatching

        val overviews = premises.map { p ->
            LibrarySpaceOverviewEntity(
                libraryId = p["id"] as? String ?: "",
                name = p["name"] as? String ?: "",
                totalSeats = (p["total_num"] as? Number)?.toInt() ?: 0,
                freeSeats = (p["free_num"] as? Number)?.toInt() ?: 0,
                isTargetLibrary = true
            )
        }
        dao.deleteAllOverviews()
        dao.insertOverviews(overviews)
        dao.deleteAllAreas()
        dao.deleteAllRooms()
    }

    suspend fun fetchAreas(libraryId: String): List<AreaInfo> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val resp = api.quickSelect(mapOf(
            "id" to "1",
            "date" to today,
            "categoryIds" to listOf("1"),
            "members" to 0
        ))
        val body = resp.body() ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any> ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val storeys = data["storey"] as? List<Map<String, Any>> ?: emptyList()
        val storeyMap = storeys.associate {
            (it["id"] as? String ?: "") to (it["name"] as? String ?: "")
        }

        @Suppress("UNCHECKED_CAST")
        val areas = data["area"] as? List<Map<String, Any>> ?: emptyList()

        return areas.filter { (it["topId"] as? String) == libraryId }
            .map { area ->
                val parentId = area["parentId"] as? String ?: ""
                val floorName = storeyMap[parentId] ?: ""
                AreaInfo(
                    areaId = area["id"] as? String ?: "",
                    name = area["name"] as? String ?: "",
                    floorName = floorName,
                    libraryName = "",
                    mergedName = area["nameMerge"] as? String ?: "",
                    totalSeats = (area["total_num"] as? Number)?.toInt() ?: 0,
                    freeSeats = (area["free_num"] as? Number)?.toInt() ?: 0,
                    typeName = ""
                )
            }
    }

    suspend fun fetchSeats(areaId: String): List<SeatInfo> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val dateResp = api.getSeatDates(mapOf("build_id" to areaId))
        val dateBody = dateResp.body() ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val dateData = dateBody["data"] as? List<Map<String, Any>> ?: return emptyList()
        if (dateData.isEmpty()) return emptyList()

        val firstDay = dateData[0]
        val day = firstDay["day"] as? String ?: today
        @Suppress("UNCHECKED_CAST")
        val times = firstDay["times"] as? List<Map<String, Any>> ?: return emptyList()
        if (times.isEmpty()) return emptyList()

        val segment = times[0]
        val segmentId = segment["id"] as? String ?: return emptyList()
        val startTime = segment["start"] as? String ?: "00:00"
        val endTime = segment["end"] as? String ?: "23:59"

        val seatResp = api.getSeats(mapOf(
            "area" to areaId,
            "segment" to segmentId,
            "day" to day,
            "startTime" to startTime,
            "endTime" to endTime
        ))
        val seatBody = seatResp.body() ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val seats = seatBody["data"] as? List<Map<String, Any>> ?: return emptyList()

        return seats.map { s ->
            SeatInfo(
                id = s["id"] as? String ?: "",
                no = s["no"] as? String ?: "",
                x = (s["point_x"] as? String)?.toFloatOrNull() ?: 0f,
                y = (s["point_y"] as? String)?.toFloatOrNull() ?: 0f,
                width = (s["width"] as? String)?.toFloatOrNull() ?: 2f,
                height = (s["height"] as? String)?.toFloatOrNull() ?: 3f,
                status = s["status"] as? String ?: "0",
                statusName = s["status_name"] as? String ?: ""
            )
        }
    }

    suspend fun fetchSeatMap(areaId: String): Map<String, String> {
        val resp = api.getSeatMap(mapOf("id" to areaId))
        val body = resp.body() ?: return emptyMap()
        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as? Map<String, Any> ?: return emptyMap()
        return data.entries.mapNotNull { (k, v) ->
            val url = v as? String
            if (url != null && url.isNotEmpty()) k to url else null
        }.toMap()
    }

    suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            imageClient.newCall(request).execute().use { resp ->
                resp.body?.bytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadImage failed: ${e.message}")
            null
        }
    }

    suspend fun getTargetLibraries() = dao.getTargetLibraries()
    suspend fun getOtherLibraries() = dao.getOtherLibraries()
    suspend fun getAreasForLibrary(libraryId: String) = dao.getAreasForLibrary(libraryId)
    suspend fun getRoomsForLibrary(libraryId: String) = dao.getRoomsForLibrary(libraryId)
}
