package com.example.tongji.ui.screens.schedule.calendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView

class CourseMonthView(context: Context) : MonthView(context) {

    private var mRadius = 0
    private val mSchemePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xFF2196F3.toInt()
    }

    override fun onPreviewHook() {
        mRadius = Math.min(mItemWidth, mItemHeight) / 10
    }

    override fun onLoopStart(x: Int, y: Int) {}

    override fun onDrawSelected(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean
    ): Boolean {
        val cx = x + mItemWidth / 2
        val cy = y + mItemHeight / 2
        canvas.drawCircle(cx.toFloat(), cy.toFloat(), mRadius * 3f, mSelectedPaint)
        return false
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {
        val cx = x + mItemWidth / 2
        val bottom = y + mItemHeight - mRadius * 2
        canvas.drawCircle(cx.toFloat(), bottom.toFloat(), mRadius.toFloat(), mSchemePaint)
    }

    override fun onDrawText(
        canvas: Canvas,
        calendar: Calendar,
        x: Int,
        y: Int,
        hasScheme: Boolean,
        isSelected: Boolean
    ) {
        val baselineY = mTextBaseLine + y
        val cx = x + mItemWidth / 2

        val paint = when {
            isSelected -> mSelectTextPaint
            hasScheme -> if (calendar.isCurrentDay) mCurDayTextPaint else mSchemeTextPaint
            else -> if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint
        }

        canvas.drawText(calendar.day.toString(), cx.toFloat(), baselineY, paint)
    }
}
