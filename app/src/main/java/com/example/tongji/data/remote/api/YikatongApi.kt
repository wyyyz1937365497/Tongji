package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface YikatongApi {
    @GET("/berserker-app/ykt/tsm/queryCard")
    suspend fun queryCard(): Response<Map<String, Any>>

    @GET("/berserker-search/search/personal/turnover")
    suspend fun getTurnover(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): Response<Map<String, Any>>
}
