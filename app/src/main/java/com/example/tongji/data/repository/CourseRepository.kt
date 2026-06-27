package com.example.tongji.data.repository

import android.util.Log
import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.local.dao.CourseScheduleDao
import com.example.tongji.data.local.entity.CourseScheduleEntity
import com.example.tongji.data.remote.api.TongjiApi
import com.example.tongji.util.StudentCodeCipher

class CourseRepository(
    private val api: TongjiApi,
    private val dao: CourseScheduleDao,
    private val credentialStore: CredentialStore
) {
    suspend fun sync(): Result<Unit> = runCatching {
        val timestamp = System.currentTimeMillis()
        val calendarResp = api.getCurrentTermCalendar(timestamp)
        val calendarBody = calendarResp.body() ?: return@runCatching
        val calendarData = calendarBody["data"] as? Map<String, Any> ?: return@runCatching
        val schoolCalendar = calendarData["schoolCalendar"] as? Map<String, Any> ?: return@runCatching
        val calendarId = (schoolCalendar["id"] as? Number)?.toInt()?.toString() ?: return@runCatching
        val beginDay = (schoolCalendar["beginDay"] as? Number)?.toLong() ?: return@runCatching
        val weekBeginDay = (schoolCalendar["weekBenginDay"] as? Number)?.toInt() ?: 2

        credentialStore.putString("calendar_begin_day", beginDay.toString())
        credentialStore.putString("calendar_week_begin_day", weekBeginDay.toString())

        val uid = credentialStore.getString(CredentialStore.KEY_UID) ?: return@runCatching
        val aesKey = credentialStore.getString(CredentialStore.KEY_AES_KEY)
        val aesIv = credentialStore.getString(CredentialStore.KEY_AES_IV)
        val studentCode = if (aesKey != null && aesIv != null) {
            StudentCodeCipher.encryptStudentCode(uid, aesKey, aesIv)
        } else {
            Log.w(TAG, "缺少 AES 密钥，无法生成 studentCode")
            return@runCatching
        }

        val response = api.findStudentTimetab(calendarId, studentCode, System.currentTimeMillis())
        if (!response.isSuccessful) {
            Log.w(TAG, "课表请求失败: ${response.code()}")
            return@runCatching
        }
        val body = response.body() ?: return@runCatching
        val list = body["data"] as? List<Map<String, Any>> ?: return@runCatching
        val schedules = parseSchedules(list, beginDay, weekBeginDay)
        dao.deleteAll()
        dao.insertAll(schedules)
        Log.d(TAG, "课表同步完成: ${schedules.size} 条")
    }

    suspend fun getSchedulesForWeek(weekNumber: Int): List<CourseScheduleEntity> {
        return dao.getSchedulesForWeek(weekNumber)
    }

    suspend fun getAllSchedules(): List<CourseScheduleEntity> {
        return dao.getAllSchedules()
    }

    suspend fun getSemesters(): List<String> {
        return dao.getSemesters()
    }

    private fun parseSchedules(courses: List<Map<String, Any>>, beginDay: Long, weekBeginDay: Int): List<CourseScheduleEntity> {
        val result = mutableListOf<CourseScheduleEntity>()
        val semester = autoDetectSemester()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
        for (course in courses) {
            val courseName = course["courseName"] as? String ?: continue
            val teacherName = course["teacherName"] as? String ?: ""
            val timeTableList = course["timeTableList"] as? List<Map<String, Any>> ?: continue
            for (timeTable in timeTableList) {
                val dayOfWeek = (timeTable["dayOfWeek"] as? Number)?.toInt() ?: continue
                val timeStart = (timeTable["timeStart"] as? Number)?.toInt() ?: continue
                val timeEnd = (timeTable["timeEnd"] as? Number)?.toInt() ?: timeStart
                val roomIdI18n = timeTable["roomIdI18n"] as? String ?: ""
                val weekNum = timeTable["weekNum"] as? String ?: ""
                val weeks = timeTable["weeks"] as? List<Number> ?: parseWeekNum(weekNum)
                val teacher = (timeTable["teacherName"] as? String)?.takeIf { it.isNotEmpty() } ?: teacherName
                for (week in weeks) {
                    val dateMillis = beginDay + (week.toInt() - 1) * 7 * 86400_000L + (dayOfWeek - weekBeginDay) * 86400_000L
                    val dateStr = sdf.format(java.util.Date(dateMillis))
                    result.add(
                        CourseScheduleEntity(
                            courseName = courseName,
                            location = roomIdI18n,
                            teacher = teacher,
                            dayOfWeek = dayOfWeek,
                            startPeriod = timeStart,
                            endPeriod = timeEnd,
                            weekNumber = week.toInt(),
                            weekText = weekNum,
                            semester = semester,
                            date = dateStr
                        )
                    )
                }
            }
        }
        return result
    }

    private fun parseWeekNum(weekNum: String): List<Int> {
        val result = mutableListOf<Int>()
        val trimmed = weekNum.trim().removePrefix("[").removeSuffix("]")
        for (part in trimmed.split(",")) {
            val p = part.trim()
            when {
                p.contains("-") -> {
                    val range = p.replace("单", "").replace("双", "").trim().split("-")
                    if (range.size == 2) {
                        val start = range[0].toIntOrNull() ?: continue
                        val end = range[1].toIntOrNull() ?: continue
                        for (w in start..end) result.add(w)
                    }
                }
                else -> p.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    private fun autoDetectSemester(): String {
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1
        return if (month >= 9) {
            "$year-${year + 1}第一学期"
        } else {
            "${year - 1}-${year}第二学期"
        }
    }

    companion object {
        private const val TAG = "CourseRepository"
    }
}
