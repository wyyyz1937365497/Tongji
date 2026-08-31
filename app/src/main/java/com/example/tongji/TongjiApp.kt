package com.example.tongji

import android.app.Application
import android.util.Log
import com.example.tongji.auth.CampusModel
import com.example.tongji.auth.CredentialStore
import com.example.tongji.data.local.AppDatabase
import com.example.tongji.data.remote.NetworkModule
import com.example.tongji.data.repository.*

class TongjiApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var courseRepository: CourseRepository
        private set
    lateinit var academicRepository: AcademicRepository
        private set
    lateinit var activityRepository: ActivityRepository
        private set
    lateinit var teachingNoticeRepository: TeachingNoticeRepository
        private set
    lateinit var yikatongRepository: YikatongRepository
        private set
    lateinit var librarySpaceRepository: LibrarySpaceRepository
        private set
    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var waterRepository: WaterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)

        val credentialStore = CredentialStore.getInstance(this)
        val tongjiApi = NetworkModule.createTongjiApi(this)
        val starApi = NetworkModule.createStarApi(this)
        val librarySpaceApi = NetworkModule.createLibrarySpaceApi(this)
        val allTongjiApi = NetworkModule.createAllTongjiApi(this)
        val yikatongApi = NetworkModule.createYikatongApi(this)
        val waterApi = NetworkModule.createWaterApi(this)

        courseRepository = CourseRepository(tongjiApi, database.courseScheduleDao(), credentialStore)
        academicRepository = AcademicRepository(tongjiApi, database.examScheduleDao(), database.gradeDao(), database.practiceHourDao(), credentialStore)
        activityRepository = ActivityRepository(starApi, database.campusActivityDao())
        teachingNoticeRepository = TeachingNoticeRepository(tongjiApi, database.teachingNoticeDao())
        yikatongRepository = YikatongRepository(yikatongApi, database.campusCardDao(), credentialStore)
        librarySpaceRepository = LibrarySpaceRepository(librarySpaceApi, database.librarySpaceDao())
        sessionRepository = SessionRepository(tongjiApi, credentialStore)
        waterRepository = WaterRepository(waterApi, credentialStore)

        val uid = credentialStore.getString(CredentialStore.KEY_UID)
        if (uid != null) {
            CampusModel.markValid()
            // 从缓存的凭证初始化 userProfile
            val name = credentialStore.getString(CredentialStore.KEY_NAME)
            Log.d("TongjiApp", "从 CredentialStore 读取 KEY_NAME: $name")
            val facultyName = credentialStore.getString(CredentialStore.KEY_FACULTY)
            val major = credentialStore.getString(CredentialStore.KEY_MAJOR)
            val grade = credentialStore.getString(CredentialStore.KEY_GRADE)
            val photoPath = credentialStore.getString(CredentialStore.KEY_PHOTO_PATH)

            val profile = com.example.tongji.auth.UserProfile(
                uid = uid,
                name = name ?: "",
                facultyName = facultyName,
                deptOrMajor = major,
                grade = grade,
                sexCode = null,
                typeCode = null,
                innerRoles = null,
                photoPath = photoPath
            )
            CampusModel.updateProfile(profile)
            Log.d("TongjiApp", "已初始化 userProfile，name='${profile.name}', uid='${profile.uid}'")
        }
    }

    companion object {
        @Volatile
        private var instance: TongjiApp? = null

        fun getInstance(): TongjiApp {
            return instance ?: throw IllegalStateException("TongjiApp not initialized")
        }
    }
}
