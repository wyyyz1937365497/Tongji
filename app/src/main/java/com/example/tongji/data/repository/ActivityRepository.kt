package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.data.local.dao.CampusActivityDao
import com.example.tongji.data.local.entity.CampusActivityEntity
import com.example.tongji.data.remote.api.StarApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActivityRepository(
    private val api: StarApi,
    private val dao: CampusActivityDao
) {
    private var preloadJob: Job? = null

    /**
     * 并行分批同步活动数据
     * @param startPage 起始页码，默认1
     * @param maxPages 最大同步页数，默认5页（250条）
     * @param clearOldData 是否清空旧数据，默认true（预加载时为false）
     * @return Result<同步的活动数量>
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun sync(startPage: Int = 1, maxPages: Int = 5, clearOldData: Boolean = true): Result<Int> = runCatching {
        val pageSize = 50
        val pageNumbers = startPage..<(startPage + maxPages)

        Log.d(TAG, "开始同步: 从第 $startPage 页开始，最多 $maxPages 页，clearOldData=$clearOldData")

        // 并行查询所有页
        val deferreds: List<Deferred<List<CampusActivityEntity>?>> = pageNumbers.map { pageNo ->
            CoroutineScope(Dispatchers.IO).async<List<CampusActivityEntity>?> {
                val resp = api.getActivityList(pageNo = pageNo, pageSize = pageSize)
                val body = resp.body()
                val code = (body?.get("code") as? Number)?.toInt()

                if (code == null || code != 0) {
                    if (code == 401 || code == 403) {
                        Log.w(TAG, "STAR 凭证失效 code=$code")
                    }
                    null  // 终止信号
                } else {
                    val data = body["data"] as? Map<String, Any>
                    val list = data?.get("list") as? List<Map<String, Any>>
                    list?.map { parseActivity(it) } ?: emptyList()
                }
            }
        }

        // 等待所有并行请求完成
        val allActivities = mutableListOf<CampusActivityEntity>()
        for (deferred in deferreds) {
            val result = deferred.await()
            if (result == null) {
                Log.d(TAG, "并行同步遇到终止信号，停止加载")
                break
            }
            if (result.isNotEmpty()) {
                allActivities.addAll(result)
                Log.d(TAG, "并行同步加载一批: ${result.size} 条")
            }
        }

        if (allActivities.isNotEmpty()) {
            if (clearOldData) {
                dao.deleteAll()
                dao.insertAll(allActivities)
            } else {
                // 预加载模式：只插入新数据，不删除旧数据
                dao.insertAll(allActivities)
            }
        }
        Log.d(TAG, "卓越星并行同步完成: ${allActivities.size} 条")
        allActivities.size
    }

    /**
     * 预加载更多活动数据
     * @param onResult 加载完成回调，返回新增的活动数量
     */
    suspend fun preloadMore(onResult: (Int) -> Unit = {}) {
        preloadJob?.cancel()
        preloadJob = CoroutineScope(Dispatchers.IO).launch {
            delay(2000)  // 延迟2秒，避免频繁请求
            val currentCount = dao.getCount()
            if (currentCount < 1000) {  // 上限 1000 条
                val currentPage = (currentCount / 50) + 1
                Log.d(TAG, "触发预加载，当前已有 $currentCount 条，从第 $currentPage 页开始")
                sync(startPage = currentPage, maxPages = 5, clearOldData = false)
                    .onSuccess { newCount ->
                        Log.d(TAG, "预加载完成: 新增 $newCount 条")
                        onResult(newCount)
                    }
            } else {
                Log.d(TAG, "已达到预加载上限 $currentCount 条，跳过预加载")
            }
        }
    }

    /**
     * 取消预加载任务
     */
    fun cancelPreload() {
        preloadJob?.cancel()
        preloadJob = null
    }

    suspend fun getAll(): List<CampusActivityEntity> = dao.getAll()

    suspend fun getFiltered(moduleCode: String?, progressName: String?): List<CampusActivityEntity> =
        dao.getFiltered(moduleCode, progressName)

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
