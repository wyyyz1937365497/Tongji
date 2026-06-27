package com.example.tongji.data.remote.model

data class SessionUserResponse(
    val uid: String?,
    val name: String?,
    val facultyName: String?,
    val deptOrMajor: String?,
    val grade: String?,
    val sexCode: String?,
    val typeCode: String?,
    val innerRoles: List<String>?,
    val photoPath: String?
)

data class CourseScheduleRaw(
    val kcmc: String?,
    val jsmc: String?,
    val xm: String?,
    val xqj: Int?,
    val dsz: Int?,
    val jcs: Int?,
    val jsz: Int?,
    val zcText: String?,
    val semester: String?
)

data class ExamItem(
    val id: Int?,
    val calendarId: Int?,
    val calendarName: String?,
    val switchRemark: String?,
    val sortIndex: Int?,
    val courseName: String?,
    val newCourseCode: String?,
    val newTeachingClassCode: String?,
    val examDateText: String?,
    val startTimeText: String?,
    val endTimeText: String?,
    val examSite: String?,
    val examTimeText: String?,
    val remark: String?,
    val examSituation: Int?,
    val isOpen: Int?
)

data class GradeSummaryRaw(
    val totalGradePoint: String?,
    val actualCredit: String?,
    val failingCredits: String?,
    val failingCourseCount: String?
)

data class GradeCourseRaw(
    val id: Int?,
    val termCode: Int?,
    val termName: String?,
    val calName: String?,
    val termAveragePoint: String?,
    val sortIndex: Int?,
    val courseCode: String?,
    val newCourseCode: String?,
    val courseName: String?,
    val courseCategory: String?,
    val creditText: String?,
    val gradePointText: String?,
    val scoreText: String?,
    val scoreExamType: String?,
    val publicCourseName: String?,
    val isPassName: String?,
    val updateTimeText: String?
)

data class StarActivityItem(
    val id: Long?,
    val title: String?,
    val source: String?,
    val activityDate: String?,
    val activityEndDate: String?,
    val location: String?,
    val link: String?,
    val descriptionText: String?,
    val progressValue: Int?,
    val progressName: String?,
    val moduleCode: String?,
    val moduleName: String?,
    val starPoints: Double?
)

data class LibraryOverviewRaw(
    val libraryId: String?,
    val name: String?,
    val totalSeats: Int?,
    val freeSeats: Int?,
    val isTargetLibrary: Boolean?
)

data class LibraryAreaRaw(
    val areaId: String?,
    val libraryId: String?,
    val libraryName: String?,
    val floorId: String?,
    val floorName: String?,
    val name: String?,
    val mergedName: String?,
    val typeName: String?,
    val totalSeats: Int?,
    val freeSeats: Int?
)

data class LibraryRoomRaw(
    val roomId: String?,
    val libraryId: String?,
    val libraryName: String?,
    val floorId: String?,
    val floorName: String?,
    val name: String?,
    val mergedName: String?,
    val typeName: String?
)
