package com.example.tongji.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.tongji.data.local.entity.CourseScheduleEntity
import com.example.tongji.data.local.entity.ExamScheduleItemEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object CalendarExporter {

    private val PERIOD_START = arrayOf(
        "", "08:00", "08:50", "10:00", "10:45",
        "11:35", "13:30", "14:15", "15:05",
        "15:50", "18:00", "18:45", "19:35", "20:10", "20:55"
    )
    private val PERIOD_END = arrayOf(
        "", "08:45", "09:35", "10:45", "11:30",
        "12:20", "14:15", "15:00", "15:50",
        "16:35", "18:45", "19:30", "20:20", "21:00", "21:45"
    )

    private data class CourseSlot(
        val courseName: String,
        val dayOfWeek: Int,
        val startPeriod: Int,
        val endPeriod: Int,
        val location: String
    )

    fun exportExams(context: Context, exams: List<ExamScheduleItemEntity>): Int {
        val resolver = context.contentResolver
        val tz = TimeZone.getDefault()
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val calId = getPrimaryCalendarId(resolver) ?: return 0
        var count = 0

        for (exam in exams) {
            val dateStr = exam.examDateText ?: continue
            val datePart = dateStr.substringBefore(" ").trim()
            if (datePart.length < 10) continue
            val startStr = exam.startTimeText?.takeIf { it.isNotEmpty() } ?: "00:00"
            val endStr = exam.endTimeText?.takeIf { it.isNotEmpty() } ?: startStr

            val startMs = try {
                timeFmt.parse("$datePart $startStr")?.time ?: continue
            } catch (_: Exception) { continue }
            val endMs = try {
                val e = timeFmt.parse("$datePart $endStr")?.time
                if (e != null && e > startMs) e else startMs + 7200_000L
            } catch (_: Exception) { startMs + 7200_000L }

            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, "${exam.courseName} 考试")
                put(CalendarContract.Events.DESCRIPTION, buildString {
                    append("学期：${exam.calendarName}")
                    if (!exam.newTeachingClassCode.isNullOrEmpty()) append("\n教学班：${exam.newTeachingClassCode}")
                    if (!exam.examTimeText.isNullOrEmpty()) append("\n${exam.examTimeText}")
                    if (!exam.remark.isNullOrEmpty()) append("\n${exam.remark}")
                })
                put(CalendarContract.Events.EVENT_LOCATION, exam.examSite ?: "")
                put(CalendarContract.Events.DTSTART, startMs)
                put(CalendarContract.Events.DTEND, endMs)
                put(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: continue
            val eventId = uri.lastPathSegment?.toLongOrNull() ?: continue

            val reminder = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 60)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
            count++
        }
        return count
    }

    fun exportCourses(context: Context, courses: List<CourseScheduleEntity>): Int {
        val resolver = context.contentResolver
        val tz = TimeZone.getDefault()
        val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val calId = getPrimaryCalendarId(resolver) ?: return 0

        val groups = courses.groupBy {
            CourseSlot(
                it.courseName,
                it.dayOfWeek,
                it.startPeriod,
                it.endPeriod,
                it.location ?: ""
            )
        }

        var count = 0
        for ((slot, items) in groups) {
            val dates = items.mapNotNull { it.date }.distinct().sorted()
            if (dates.isEmpty()) continue
            val startTime = PERIOD_START.getOrNull(slot.startPeriod) ?: continue
            val endTime = PERIOD_END.getOrNull(slot.endPeriod) ?: startTime
            val firstDate = dates.first()

            val startMs = try {
                timeFmt.parse("$firstDate $startTime")?.time ?: continue
            } catch (_: Exception) { continue }
            val endMs = try {
                timeFmt.parse("$firstDate $endTime")?.time ?: startMs + 2700_000L
            } catch (_: Exception) { startMs + 2700_000L }

            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, slot.courseName)
                put(CalendarContract.Events.DESCRIPTION, buildString {
                    val teacher = items.firstOrNull()?.teacher
                    if (!teacher.isNullOrEmpty()) append("教师：$teacher\n")
                    append("周次：第${items.minOf { it.weekNumber }}-${items.maxOf { it.weekNumber }}周（共${dates.size}次）\n")
                    append("节次：第${slot.startPeriod}-${slot.endPeriod}节\n")
                    append("来源：同济1系统课表")
                })
                if (slot.location.isNotEmpty()) {
                    put(CalendarContract.Events.EVENT_LOCATION, slot.location)
                }
                put(CalendarContract.Events.DTSTART, startMs)
                put(CalendarContract.Events.DTEND, endMs)
                put(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.HAS_ALARM, 1)
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=${dates.size}")
            }
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) count++
        }
        return count
    }

    private fun getPrimaryCalendarId(resolver: ContentResolver): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            var primaryId: Long? = null
            var firstValidId: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                val owner = cursor.getString(2)
                if (firstValidId == null && !owner.isNullOrEmpty()) firstValidId = id
                if (isPrimary) { primaryId = id; break }
            }
            return primaryId ?: firstValidId
        }
        return null
    }
}
