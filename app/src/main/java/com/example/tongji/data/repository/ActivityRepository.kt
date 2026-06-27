package com.example.tongji.data.repository

import com.example.tongji.data.local.dao.CampusActivityDao
import com.example.tongji.data.local.entity.CampusActivityEntity
import com.example.tongji.data.remote.api.StarApi
import java.text.SimpleDateFormat
import java.util.*

class ActivityRepository(
    private val api: StarApi,
    private val dao: CampusActivityDao
) {
    suspend fun sync(): Result<Unit> = runCatching {
        var page = 1
        val allActivities = mutableListOf<CampusActivityEntity>()
        while (true) {
            val resp = api.getActivityList(page = page, size = 50)
            val body = resp.body() ?: break
            val list = body["list"] as? List<Map<String, Any>> ?: break
            if (list.isEmpty()) break
            allActivities.addAll(list.map { parseActivity(it) })
            page++
        }
        dao.deleteAll()
        dao.insertAll(allActivities)
    }

    suspend fun getAll(): List<CampusActivityEntity> = dao.getAll()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

    private fun parseActivity(item: Map<String, Any>): CampusActivityEntity {
        val id = (item["id"] as? Number)?.toLong() ?: 0L
        val activityDate = parseDate(item["activityDate"] as? String) ?: System.currentTimeMillis()
        val activityEndDate = parseDate(item["activityEndDate"] as? String)

        val moduleCode = item["moduleCode"] as? String
        val moduleName = item["moduleName"] as? String
        val points = (item["starPoints"] as? Number)?.toDouble() ?: 0.0
        val progressValue = (item["progressValue"] as? Number)?.toInt()
        val progressName = item["progressName"] as? String

        val source = item["source"] as? String ?: ""
        val description = buildString {
            if (moduleName != null) append("模块：$moduleName\n")
            if (progressName != null) append("状态：$progressName\n")
            append("积分：$points")
        }

        return CampusActivityEntity(
            remoteId = id,
            title = item["title"] as? String ?: "",
            source = source,
            activityDate = activityDate,
            activityEndDate = activityEndDate,
            location = item["location"] as? String,
            link = item["link"] as? String,
            descriptionText = description,
            progressValue = progressValue,
            progressName = progressName,
            moduleCode = moduleCode,
            moduleName = moduleName,
            starPoints = points
        )
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
