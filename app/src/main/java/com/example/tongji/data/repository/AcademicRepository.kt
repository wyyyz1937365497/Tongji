package com.example.tongji.data.repository

import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.local.dao.ExamScheduleDao
import com.example.tongji.data.local.dao.GradeDao
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import com.example.tongji.data.local.entity.GradeCourseRecordEntity
import com.example.tongji.data.local.entity.GradeSummaryEntity
import com.example.tongji.data.remote.api.TongjiApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AcademicRepository(
    private val api: TongjiApi,
    private val examDao: ExamScheduleDao,
    private val gradeDao: GradeDao,
    private val credentialStore: CredentialStore
) {
    suspend fun syncExams(): Result<Unit> = runCatching {
        api.switchAuthContext(mapOf("authId" to 9102))
        val formBody = "1".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        val calendarResp = api.getExamCalendar(formBody)
        val calendarBody = calendarResp.body() ?: return@runCatching
        val calendarData = calendarBody["data"] as? Map<String, Any> ?: return@runCatching
        val calendarId = (calendarData["calendarId"] as? Number)?.toInt() ?: return@runCatching

        var page = 1
        val allExams = mutableListOf<ExamScheduleItemEntity>()
        var switchRemark = ""
        while (true) {
            val resp = api.getExamListPage(mapOf(
                "pageNum_" to page,
                "pageSize_" to 20,
                "condition" to mapOf(
                    "calendarId" to calendarId,
                    "examSituation" to "",
                    "examType" to 1
                )
            ))
            val body = resp.body() ?: break
            val wrapper = body["data"] as? Map<String, Any> ?: break
            if (switchRemark.isEmpty()) {
                switchRemark = wrapper["switchRrmark"] as? String ?: ""
            }
            val pageWrapper = wrapper["data"] as? Map<String, Any> ?: break
            val list = pageWrapper["list"] as? List<Map<String, Any>> ?: break
            if (list.isEmpty()) break
            allExams.addAll(list.map { parseExamItem(it, calendarId, switchRemark) })
            val total = (pageWrapper["total_"] as? Number)?.toInt() ?: list.size
            val pageSize = (pageWrapper["pageSize_"] as? Number)?.toInt() ?: 20
            if (page * pageSize >= total) break
            page++
        }
        examDao.deleteAll()
        examDao.insertAll(allExams)
    }

    suspend fun syncGrades(): Result<Unit> = runCatching {
        val studentId = credentialStore.getString(CredentialStore.KEY_UID)
            ?: return@runCatching
        api.switchAuthContext(mapOf("authId" to 12174))
        val timestamp = System.currentTimeMillis()
        val gradesResp = api.getMyGrades(studentId, timestamp)
        val gradesBody = gradesResp.body() ?: return@runCatching
        val data = gradesBody["data"] as? Map<String, Any> ?: return@runCatching

        gradeDao.insertSummary(GradeSummaryEntity(
            totalGradePoint = data["totalGradePoint"] as? String ?: "0",
            actualCredit = data["actualCredit"] as? String ?: "0",
            failingCredits = data["failingCredits"] as? String ?: "0",
            failingCourseCount = data["failingCourseCount"] as? String ?: "0"
        ))

        val terms = data["term"] as? List<Map<String, Any>> ?: emptyList()
        val allCourses = mutableListOf<GradeCourseRecordEntity>()
        for (term in terms) {
            val termCode = (term["termcode"] as? Number)?.toInt() ?: 0
            val termName = term["termName"] as? String ?: ""
            val calName = term["calName"] as? String ?: ""
            val termAvg = term["averagePoint"] as? String ?: ""
            val creditInfo = term["creditInfo"] as? List<Map<String, Any>> ?: emptyList()
            for (course in creditInfo) {
                allCourses.add(parseGradeCourse(course, termCode, termName, calName, termAvg))
            }
        }
        gradeDao.deleteAllCourses()
        gradeDao.insertAllCourses(allCourses)

        val tagsResp = api.queryCourseTag(studentId, timestamp)
        if (tagsResp.isSuccessful) {
            val tagsBody = tagsResp.body()
            val tags = tagsBody?.get("data") as? List<Map<String, Any>> ?: emptyList()
            val tagMap = tags.associate {
                (it["id"] as? Number)?.toInt() to (it["shortName"] as? String)
            }
        }
    }

    suspend fun getScheduledExams(): List<ExamScheduleItemEntity> = examDao.getScheduledExams()
    suspend fun getUnscheduledExams(): List<ExamScheduleItemEntity> = examDao.getUnscheduledExams()

    suspend fun getSummary(): GradeSummaryEntity? = gradeDao.getSummary()
    suspend fun getTerms() = gradeDao.getTerms()
    suspend fun getCoursesForTerm(termCode: Int) = gradeDao.getCoursesForTerm(termCode)

    private fun parseExamItem(item: Map<String, Any>, calendarId: Int, switchRemark: String): ExamScheduleItemEntity {
        return ExamScheduleItemEntity(
            sourceId = (item["id"] as? Number)?.toInt() ?: 0,
            calendarId = calendarId,
            calendarName = "",
            switchRemark = switchRemark,
            sortIndex = (item["sortIndex"] as? Number)?.toInt() ?: 0,
            courseName = item["courseName"] as? String ?: "",
            newCourseCode = item["newCourseCode"] as? String ?: "",
            newTeachingClassCode = item["newTeachingClassCode"] as? String ?: "",
            examDateText = item["examDate"] as? String,
            startTimeText = item["startTime"] as? String,
            endTimeText = item["endTime"] as? String,
            examSite = item["examSite"] as? String,
            examTimeText = item["examTime"] as? String,
            remark = item["remark"] as? String,
            examSituation = (item["examSituation"] as? Number)?.toInt() ?: 0,
            isOpen = (item["isOpen"] as? Number)?.toInt() ?: 0
        )
    }

    private fun parseGradeCourse(item: Map<String, Any>, termCode: Int, termName: String, calName: String, termAvg: String): GradeCourseRecordEntity {
        return GradeCourseRecordEntity(
            sourceId = (item["id"] as? Number)?.toInt() ?: 0,
            termCode = termCode,
            termName = termName,
            calName = calName,
            termAveragePoint = termAvg,
            sortIndex = (item["sortIndex"] as? Number)?.toInt() ?: 0,
            courseCode = item["courseCode"] as? String ?: "",
            newCourseCode = item["newCourseCode"] as? String ?: "",
            courseName = item["courseName"] as? String ?: "",
            courseCategory = item["courseLabel"] as? String,
            creditText = cleanNumber(item["credit"]),
            gradePointText = cleanNumber(item["gradePoint"]),
            scoreText = (item["scoreName"] as? String)?.takeIf { it.isNotEmpty() } ?: (item["score"] as? String),
            scoreExamType = item["scoreEaxmTypeI18n"] as? String,
            publicCourseName = item["publicCoursesName"] as? String,
            isPassName = item["isPassName"] as? String,
            updateTimeText = item["updateTime"] as? String
        )
    }

    private fun cleanNumber(value: Any?): String {
        if (value == null) return ""
        return when (value) {
            is Number -> {
                val d = value.toDouble()
                if (d == d.toInt().toDouble()) d.toInt().toString() else String.format("%.1f", d)
            }
            is String -> value
            else -> value.toString()
        }
    }
}
