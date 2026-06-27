package com.example.tongji.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tongji.data.local.dao.*
import com.example.tongji.data.local.entity.*

@Database(
    entities = [
        CourseScheduleEntity::class,
        ExamScheduleItemEntity::class,
        GradeSummaryEntity::class,
        GradeCourseRecordEntity::class,
        CampusActivityEntity::class,
        CampusCardBalanceEntity::class,
        CampusCardTransactionEntity::class,
        LibrarySpaceOverviewEntity::class,
        LibrarySpaceAreaEntity::class,
        LibrarySpaceRoomEntity::class,
        TeachingNoticeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseScheduleDao(): CourseScheduleDao
    abstract fun examScheduleDao(): ExamScheduleDao
    abstract fun gradeDao(): GradeDao
    abstract fun campusActivityDao(): CampusActivityDao
    abstract fun campusCardDao(): CampusCardDao
    abstract fun librarySpaceDao(): LibrarySpaceDao
    abstract fun teachingNoticeDao(): TeachingNoticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tongji_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
