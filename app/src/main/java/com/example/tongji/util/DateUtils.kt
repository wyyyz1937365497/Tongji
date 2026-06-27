package com.example.tongji.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private val displayDateFormat = SimpleDateFormat("MM月dd日", Locale.CHINA)
    private val displayDateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.CHINA)

    fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (_: Exception) {
            try {
                dateTimeFormat.parse(dateStr)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return displayDateTimeFormat.format(Date(timestamp))
    }

    fun getCurrentWeekNumber(semesterStartDate: String?): Int {
        if (semesterStartDate == null) return 1
        val startTime = parseDate(semesterStartDate) ?: return 1
        val diff = System.currentTimeMillis() - startTime
        val days = diff / (1000 * 60 * 60 * 24)
        return (days / 7).toInt() + 1
    }

    val currentSemester: String
        get() {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            return if (month >= 9) {
                "$year-${year + 1}第一学期"
            } else {
                "${year - 1}-${year}第二学期"
            }
        }
}
