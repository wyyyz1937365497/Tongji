package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface StarApi {
    @GET("/api/app-api/activity/index/list")
    suspend fun getActivityList(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<Map<String, Any>>

    @GET("/api/app-api/activity/statistics/start-count")
    suspend fun getStarScoreSummary(): Response<Map<String, Any>>
}
