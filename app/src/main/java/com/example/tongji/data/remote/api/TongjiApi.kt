package com.example.tongji.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface TongjiApi {

    @POST("/api/sessionservice/session/currentAuthId")
    suspend fun switchAuthContext(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @GET("/api/sessionservice/session/getSessionUser")
    suspend fun getSessionUser(): Response<Map<String, Any>>

    @POST("/api/electionservice/underGraduateExamSwitch/getExamCalendar")
    suspend fun getExamCalendar(@Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap()): Response<Map<String, Any>>

    @POST("/api/electionservice/undergraduateExamQuery/getStudentListPage")
    suspend fun getExamListPage(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @GET("/api/scoremanagementservice/studentScoreBk/queryCourseTag")
    suspend fun queryCourseTag(): Response<List<Map<String, Any>>>

    @GET("/api/scoremanagementservice/scoreGrades/getMyGrades")
    suspend fun getMyGrades(): Response<Map<String, Any>>

    @GET("/api/electionservice/reportManagement/findStudentTimetab")
    suspend fun findStudentTimetab(): Response<Map<String, Any>>

    @GET("/api/baseresservice/schoolCalendar/currentTermCalendar")
    suspend fun getCurrentTermCalendar(): Response<Map<String, Any>>

    @POST("/api/commonservice/commonMsgPublish/findMyCommonMsgPublish")
    suspend fun findMyCommonMsgPublish(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, Any>>

    @GET("/api/commonservice/commonMsgPublish/findCommonMsgPublishById")
    suspend fun findCommonMsgPublishById(@Query("id") id: String): Response<Map<String, Any>>

    @GET("/api/commonservice/obsfile/downloadfile")
    suspend fun downloadAttachment(@Query("objectkey") objectKey: String): Response<okhttp3.ResponseBody>
}
