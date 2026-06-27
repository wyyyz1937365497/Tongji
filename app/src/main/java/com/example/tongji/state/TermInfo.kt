package com.example.tongji.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object TermInfo {
    var simpleName by mutableStateOf<String?>(null)
        private set
    var currentWeek by mutableStateOf<Int?>(null)
        private set
    var calendarId by mutableStateOf<Int?>(null)
        private set
    var term by mutableStateOf<Int?>(null)
        private set

    fun update(simpleName: String?, week: Int?, calendarId: Int?, term: Int?) {
        if (simpleName != null) this.simpleName = simpleName
        if (week != null) this.currentWeek = week
        if (calendarId != null) this.calendarId = calendarId
        if (term != null) this.term = term
    }

    fun isCurrentTerm(termCode: Int): Boolean = calendarId == termCode
}
