package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AllTongjiApi {
    @GET("/custom/api/v1/rt/card/card_balance")
    suspend fun getCardBalance(@Query("userId") userId: String): Response<Map<String, Any>>
}
