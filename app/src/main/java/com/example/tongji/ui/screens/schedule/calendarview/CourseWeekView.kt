package com.example.tongji.ui.screens.schedule.calendarview

import android.content.Context
import android.graphics.Canvas
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.WeekView

class CourseWeekView(context: Context) : WeekView(context) {

    private var mDotRadius = 0

    override fun onPreviewHook() {
        mDotRadius = Math.min(mItemWidth, mItemHeight) / 10
    }

    override fun onDrawSelected(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        hasScheme: Boolean
    ): Boolean {
        val cx = x + mItemWidth / 2
        val cy = mItemHeight / 2
        canvas.drawCircle(cx.toFloat(), cy.toFloat(), mDotRadius * 3f, mSelectedPaint)
        return false
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int) {
        val cx = x + mItemWidth / 2
        val bottom = mItemHeight - mDotRadius * 2
        canvas.drawCircle(cx.toFloat(), bottom.toFloat(), mDotRadius.toFloat(), mSchemePaint)
    }

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        val baselineY = mTextBaseLine
        val cx = x + mItemWidth / 2

        val paint = when {
            isSelected -> mSelectTextPaint
            hasScheme -> if (calendar.isCurrentDay) mCurDayTextPaint else mSchemeTextPaint
            else -> if (calendar.isCurrentDay) mCurDayTextPaint else mCurMonthTextPaint
        }

        canvas.drawText(calendar.day.toString(), cx.toFloat(), baselineY, paint)
    }
}
