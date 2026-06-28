package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface YikatongApi {
    @POST("/User/GetCardInfoByAccountNoParm")
    @FormUrlEncoded
    suspend fun getCardInfo(@Field("json") json: String = "true"): Response<Map<String, Any>>

    @POST("/User/GetCardAccInfo")
    @FormUrlEncoded
    suspend fun getCardAccInfo(
        @Field("acc") account: String,
        @Field("json") json: String = "true"
    ): Response<Map<String, Any>>

    @POST("/Report/GetPersonTrjn")
    @FormUrlEncoded
    suspend fun getPersonTrjn(
        @Field("sdate") startDate: String,
        @Field("edate") endDate: String,
        @Field("account") account: String,
        @Field("page") page: Int = 1,
        @Field("rows") rows: Int = 50
    ): Response<Map<String, Any>>
}
