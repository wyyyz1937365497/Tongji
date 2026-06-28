package com.example.tongji.data.remote.api

import com.example.tongji.data.remote.model.WaterControllerResponse
import com.example.tongji.data.remote.model.WaterGroupResponse
import com.example.tongji.data.remote.model.WaterSsoCheckResponse
import com.example.tongji.data.remote.model.WaterTokenResponse
import retrofit2.Response
import retrofit2.http.*

interface WaterApi {
    @POST("/accountapi/berserker-base/user/Sso/SsoCheck")
    @FormUrlEncoded
    suspend fun ssoCheck(@Field("ticket") ticket: String): Response<WaterSsoCheckResponse>

    @GET("/waterapi/api/UseHzWatch")
    suspend fun getGroups(): Response<WaterGroupResponse>

    @GET("/waterapi/api/GetToken")
    suspend fun getToken(@Query("info") info: String): Response<WaterTokenResponse>

    @GET("/waterapi/api/AccUseHzWatch")
    suspend fun getControllers(
        @Query("info") info: String,
        @Query("token") token: String
    ): Response<WaterControllerResponse>
}