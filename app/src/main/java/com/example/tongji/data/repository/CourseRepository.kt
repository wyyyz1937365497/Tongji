package com.example.tongji.data.repository

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

        val uid = credentialStore.getString(CredentialStore.KEY_UID) ?: return@runCatching
        val aesKey = credentialStore.getString(CredentialStore.KEY_AES_KEY)
        val aesIv = credentialStore.getString(CredentialStore.KEY_AES_IV)
        val studentCode = if (aesKey != null && aesIv != null) {
            StudentCodeCipher.encryptStudentCode(uid, aesKey, aesIv)
        } else {
            return@runCatching
        }

        val response = api.findStudentTimetab(calendarId, studentCode, System.currentTimeMillis())
        if (response.isSuccessful) {
            val body = response.body() ?: return@runCatching
            val data = body["data"] as? Map<String, Any> ?: return@runCatching
            val list = data["list"] as? List<Map<String, Any>> ?: return@runCatching
            val schedules = parseSchedules(list)
            dao.deleteAll()
            dao.insertAll(schedules)
        }
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

    private fun parseSchedules(list: List<Map<String, Any>>): List<CourseScheduleEntity> {
        return list.mapNotNull { item ->
            try {
                val weekText = item["zcText"] as? String ?: return@mapNotNull null
                val dayOfWeek = (item["xqj"] as? Number)?.toInt() ?: return@mapNotNull null
                val startPeriod = (item["dsz"] as? Number)?.toInt()
                    ?: (item["jcs"] as? Number)?.toInt() ?: return@mapNotNull null
                val endPeriod = (item["jsz"] as? Number)?.toInt() ?: startPeriod

                val weeks = parseWeekText(weekText)
                val courseName = item["kcmc"] as? String ?: ""
                val location = item["jsmc"] as? String ?: ""
                val teacher = item["xm"] as? String ?: ""
                val semester = (item["semester"] as? String) ?: autoDetectSemester()

                weeks.map { week ->
                    CourseScheduleEntity(
                        courseName = courseName,
                        location = location,
                        teacher = teacher,
                        dayOfWeek = dayOfWeek,
                        startPeriod = startPeriod,
                        endPeriod = endPeriod,
                        weekNumber = week,
                        weekText = weekText,
                        semester = semester
                    )
                }
            } catch (_: Exception) {
                null
            }
        }.flatten()
    }

    private fun parseWeekText(text: String): List<Int> {
        val result = mutableListOf<Int>()
        val parts = text.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            when {
                trimmed.contains("-") -> {
                    val isOdd = trimmed.contains("单")
                    val isEven = trimmed.contains("双")
                    val clean = trimmed.replace("单", "").replace("双", "").trim()
                    val range = clean.split("-")
                    if (range.size == 2) {
                        val start = range[0].trim().toIntOrNull() ?: continue
                        val end = range[1].trim().toIntOrNull() ?: continue
                        for (w in start..end) {
                            if (isOdd && w % 2 == 1) result.add(w)
                            else if (isEven && w % 2 == 0) result.add(w)
                            else if (!isOdd && !isEven) result.add(w)
                        }
                    }
                }
                else -> {
                    trimmed.toIntOrNull()?.let { result.add(it) }
                }
            }
        }
        return result.distinct().sorted()
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
}
