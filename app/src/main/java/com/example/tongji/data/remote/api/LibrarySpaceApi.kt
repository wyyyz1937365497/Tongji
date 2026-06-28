package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface LibrarySpaceApi {
    @POST("/api/cas/user")
    suspend fun casUser(@Body body: Map<String, @JvmSuppressWildcards String>): Response<Map<String, Any>>

    @POST("/reserve/index/quickSelect")
    suspend fun quickSelect(@Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<Map<String, Any>>

    @POST("/reserve/index/index")
    suspend fun reserveIndex(@Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<Map<String, Any>>

    @POST("/api/seat/label")
    suspend fun getSeatLabels(@Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<Map<String, Any>>

    @POST("/api/seat/map")
    suspend fun getSeatMap(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @POST("/api/Seat/date")
    suspend fun getSeatDates(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @POST("/api/Seat/seat")
    suspend fun getSeats(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @POST("/api/Seminar/detail")
    suspend fun getSeminarDetail(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @POST("/api/Seminar/v1seminar")
    suspend fun getSeminarAvailability(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>
}
