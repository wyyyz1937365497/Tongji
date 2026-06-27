package com.example.tongji.data.repository

import com.example.tongji.data.local.dao.ExamScheduleDao
import com.example.tongji.data.local.dao.GradeDao
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import com.example.tongji.data.local.entity.GradeCourseRecordEntity
import com.example.tongji.data.local.entity.GradeSummaryEntity
import com.example.tongji.data.remote.api.TongjiApi

class AcademicRepository(
    private val api: TongjiApi,
    private val examDao: ExamScheduleDao,
    private val gradeDao: GradeDao
) {
    suspend fun syncExams(): Result<Unit> = runCatching {
        api.switchAuthContext(mapOf("authId" to 9102))
        val calendarResp = api.getExamCalendar()
        val calendarData = calendarResp.body() ?: return@runCatching
        val calendarId = (calendarData["id"] as? Number)?.toInt() ?: return@runCatching

        var page = 1
        val allExams = mutableListOf<ExamScheduleItemEntity>()
        while (true) {
            val resp = api.getExamListPage(mapOf(
                "calendarId" to calendarId,
                "page" to page,
                "pageSize" to 50
            ))
            val body = resp.body() ?: break
            val list = body["list"] as? List<Map<String, Any>> ?: break
            if (list.isEmpty()) break
            allExams.addAll(list.map { parseExamItem(it) })
            page++
        }
        examDao.deleteAll()
        examDao.insertAll(allExams)
    }

    suspend fun syncGrades(): Result<Unit> = runCatching {
        api.switchAuthContext(mapOf("authId" to 12174))
        val gradesResp = api.getMyGrades()
        val gradesBody = gradesResp.body() ?: return@runCatching

        val summary = gradesBody["summary"] as? Map<String, Any>
        if (summary != null) {
            gradeDao.insertSummary(GradeSummaryEntity(
                totalGradePoint = summary["totalGradePoint"] as? String ?: "0",
                actualCredit = summary["actualCredit"] as? String ?: "0",
                failingCredits = summary["failingCredits"] as? String ?: "0",
                failingCourseCount = summary["failingCourseCount"] as? String ?: "0"
            ))
        }

        val courseList = gradesBody["list"] as? List<Map<String, Any>> ?: emptyList()
        val courses = courseList.map { parseGradeCourse(it) }
        gradeDao.deleteAllCourses()
        gradeDao.insertAllCourses(courses)

        val tagsResp = api.queryCourseTag()
        if (tagsResp.isSuccessful) {
            val tags = tagsResp.body() ?: emptyList()
            val tagMap = tags.associate { (it["id"] as? Number)?.toInt() to (it["shortName"] as? String) }
        }
    }

    suspend fun getScheduledExams(): List<ExamScheduleItemEntity> = examDao.getScheduledExams()
    suspend fun getUnscheduledExams(): List<ExamScheduleItemEntity> = examDao.getUnscheduledExams()

    suspend fun getSummary(): GradeSummaryEntity? = gradeDao.getSummary()
    suspend fun getTerms() = gradeDao.getTerms()
    suspend fun getCoursesForTerm(termCode: Int) = gradeDao.getCoursesForTerm(termCode)

    private fun parseExamItem(item: Map<String, Any>): ExamScheduleItemEntity {
        return ExamScheduleItemEntity(
            sourceId = (item["id"] as? Number)?.toInt() ?: 0,
            calendarId = (item["calendarId"] as? Number)?.toInt() ?: 0,
            calendarName = item["calendarName"] as? String ?: "",
            switchRemark = item["switchRemark"] as? String ?: "",
            sortIndex = (item["sortIndex"] as? Number)?.toInt() ?: 0,
            courseName = item["courseName"] as? String ?: "",
            newCourseCode = item["newCourseCode"] as? String ?: "",
            newTeachingClassCode = item["newTeachingClassCode"] as? String ?: "",
            examDateText = item["examDateText"] as? String,
            startTimeText = item["startTimeText"] as? String,
            endTimeText = item["endTimeText"] as? String,
            examSite = item["examSite"] as? String,
            examTimeText = item["examTimeText"] as? String,
            remark = item["remark"] as? String,
            examSituation = (item["examSituation"] as? Number)?.toInt() ?: 0,
            isOpen = (item["isOpen"] as? Number)?.toInt() ?: 0
        )
    }

    private fun parseGradeCourse(item: Map<String, Any>): GradeCourseRecordEntity {
        return GradeCourseRecordEntity(
            sourceId = (item["id"] as? Number)?.toInt() ?: 0,
            termCode = (item["termCode"] as? Number)?.toInt() ?: 0,
            termName = item["termName"] as? String ?: "",
            calName = item["calName"] as? String ?: "",
            termAveragePoint = item["termAveragePoint"] as? String ?: "",
            sortIndex = (item["sortIndex"] as? Number)?.toInt() ?: 0,
            courseCode = item["courseCode"] as? String ?: "",
            newCourseCode = item["newCourseCode"] as? String ?: "",
            courseName = item["courseName"] as? String ?: "",
            courseCategory = item["courseCategory"] as? String,
            creditText = item["creditText"] as? String ?: "0",
            gradePointText = item["gradePointText"] as? String,
            scoreText = item["scoreText"] as? String,
            scoreExamType = item["scoreExamType"] as? String,
            publicCourseName = item["publicCourseName"] as? String,
            isPassName = item["isPassName"] as? String,
            updateTimeText = item["updateTimeText"] as? String
        )
    }
}
