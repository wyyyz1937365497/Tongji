package com.example.tongji.ui.screens.schedule.calendarview

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import com.haibin.calendarview.CalendarView

object CalendarThemeApplier {

    fun apply(calendarView: CalendarView, scheme: ColorScheme) {
        val primary = scheme.primary.toArgb()
        val onPrimary = scheme.onPrimary.toArgb()
        val onSurface = scheme.onSurface.toArgb()
        val onSurfaceVariant = scheme.onSurfaceVariant.toArgb()
        val surface = scheme.surface.toArgb()
        val primaryContainer = scheme.primaryContainer.toArgb()
        val onPrimaryContainer = scheme.onPrimaryContainer.toArgb()
        val tertiary = scheme.tertiary.toArgb()

        calendarView.setTextColor(
            primary,
            onSurface,
            onSurfaceVariant,
            onSurfaceVariant,
            onSurfaceVariant
        )
        calendarView.setSelectedColor(primary, onPrimary, onPrimary)
        calendarView.setThemeColor(primary, primary)
        calendarView.setSchemeColor(primary, primary, onPrimary)
        calendarView.setWeeColor(surface, onSurfaceVariant)
    }
}
