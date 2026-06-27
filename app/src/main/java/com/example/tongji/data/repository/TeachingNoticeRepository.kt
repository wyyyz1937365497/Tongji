package com.example.tongji.data.repository

import com.example.tongji.data.local.dao.TeachingNoticeDao
import com.example.tongji.data.local.entity.TeachingNoticeEntity
import com.example.tongji.data.remote.api.TongjiApi

class TeachingNoticeRepository(
    private val api: TongjiApi,
    private val dao: TeachingNoticeDao
) {
    suspend fun sync(): Result<Unit> = runCatching {
        var page = 1
        val allNotices = mutableListOf<TeachingNoticeEntity>()
        while (true) {
            val resp = api.findMyCommonMsgPublish(mapOf(
                "total" to 0,
                "pageNum_" to page,
                "pageSize_" to 50
            ))
            val body = resp.body() ?: break
            val data = body["data"] as? Map<String, Any> ?: break
            val list = data["list"] as? List<Map<String, Any>> ?: break
            if (list.isEmpty()) break
            allNotices.addAll(list.map { parseNotice(it) })
            val total = (data["total_"] as? Number)?.toInt() ?: list.size
            val pageSize = (data["pageSize_"] as? Number)?.toInt() ?: 50
            if (page * pageSize >= total) break
            page++
        }
        dao.deleteAll()
        dao.insertAll(allNotices)
    }

    suspend fun getAll(): List<TeachingNoticeEntity> = dao.getAll()

    suspend fun getDetail(id: String): Map<String, Any>? {
        val resp = api.findCommonMsgPublishById(id, System.currentTimeMillis())
        if (!resp.isSuccessful) return null
        val body = resp.body() ?: return null
        return body["data"] as? Map<String, Any>
    }

    suspend fun markAsRead(id: String) = dao.markAsRead(id)

    private fun parseNotice(item: Map<String, Any>): TeachingNoticeEntity {
        return TeachingNoticeEntity(
            id = (item["id"] as? Number)?.toInt()?.toString() ?: (item["id"] as? String ?: ""),
            title = item["title"] as? String ?: "",
            publishTimeText = item["publishTime"] as? String ?: "",
            topStatus = (item["topStatus"] as? Number)?.toInt() ?: 0
        )
    }
}
