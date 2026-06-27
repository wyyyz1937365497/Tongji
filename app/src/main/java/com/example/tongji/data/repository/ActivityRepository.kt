package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.data.local.dao.CampusActivityDao
import com.example.tongji.data.local.entity.CampusActivityEntity
import com.example.tongji.data.remote.api.StarApi

class ActivityRepository(
    private val api: StarApi,
    private val dao: CampusActivityDao
) {
    suspend fun sync(): Result<Unit> = runCatching {
        val allActivities = mutableListOf<CampusActivityEntity>()
        var pageNo = 1
        val pageSize = 10
        while (true) {
            val resp = api.getActivityList(pageNo = pageNo, pageSize = pageSize)
            val body = resp.body() ?: break
            val code = (body["code"] as? Number)?.toInt()
            if (code != null && code != 0) {
                if (code == 401 || code == 403) {
                    Log.w(TAG, "STAR 凭证失效 code=$code")
                }
                break
            }
            val data = body["data"] as? Map<String, Any> ?: break
            val list = data["list"] as? List<Map<String, Any>> ?: break
            if (list.isEmpty()) break
            allActivities.addAll(list.map { parseActivity(it) })
            if (list.size < pageSize) break
            pageNo++
        }
        dao.deleteAll()
        dao.insertAll(allActivities)
        Log.d(TAG, "卓越星同步完成: ${allActivities.size} 条")
    }

    suspend fun getAll(): List<CampusActivityEntity> = dao.getAll()

    private fun parseActivity(item: Map<String, Any>): CampusActivityEntity {
        val id = (item["id"] as? Number)?.toLong() ?: 0L
        val moduleCode = item["module"] as? String
        val moduleName = moduleCode?.let { CATEGORY_NAME_MAP[it] } ?: moduleCode
        val progress = item["progress"] as? Map<String, Any>
        val progressValue = (progress?.get("value") as? Number)?.toInt()
        val progressName = progress?.get("name") as? String
        val points = (item["points"] as? Number)?.toDouble() ?: 0.0
        val source = item["mainBoardUnit"] as? String ?: "STAR平台"
        val location = item["addr"] as? String
        val startTime = (item["activityStartTime"] as? Number)?.toLong()
        val endTime = (item["activityEndTime"] as? Number)?.toLong()
        val photo = item["photo"] as? String
        val pageViews = (item["pageViews"] as? Number)?.toInt() ?: 0
        val enrolled = (item["enrolledNumber"] as? Number)?.toInt() ?: 0

        val descParts = mutableListOf<String>()
        moduleName?.let { descParts.add(it) }
        if (!progressName.isNullOrEmpty()) descParts.add(progressName)
        if (points > 0) descParts.add("星值: ${formatPoints(points)}")
        if (pageViews > 0) descParts.add("浏览: $pageViews")

        return CampusActivityEntity(
            remoteId = id,
            title = item["title"] as? String ?: "",
            source = source,
            activityDate = startTime,
            activityEndDate = endTime,
            location = location,
            link = if (id > 0) "https://star.tongji.edu.cn/app/pages-home/detail/huodong?id=$id" else null,
            descriptionText = descParts.joinToString(" | ").takeIf { it.isNotEmpty() },
            progressValue = progressValue,
            progressName = progressName,
            moduleCode = moduleCode,
            moduleName = moduleName,
            starPoints = points
        )
    }

    private fun formatPoints(value: Double): String =
        if (value == value.toInt().toDouble()) value.toInt().toString()
        else String.format("%.1f", value)

    companion object {
        private const val TAG = "ActivityRepository"
        val CATEGORY_NAME_MAP: Map<String, String> = mapOf(
            "lixing" to "力行之星",
            "qiusuo" to "求索之星",
            "hongwen" to "弘文之星",
            "mingde" to "明德之星",
            "shizhi" to "矢志之星"
        )
    }
}
