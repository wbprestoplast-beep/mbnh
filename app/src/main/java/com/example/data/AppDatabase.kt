package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class,
        DocumentEntity::class,
        ClinicalNoteEntity::class,
        CarePlanEntity::class,
        AttendanceEntity::class,
        VitalRecordEntity::class,
        NotificationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hospitalDao(): HospitalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return getDatabase(context, CoroutineScope(Dispatchers.IO))
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mb_nursing_home.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            INSTANCE?.let { database ->
                                seedDatabase(database.hospitalDao())
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabase(dao: HospitalDao) {
            if (dao.getUsersCount() == 0) {
                dao.insertUsers(HospitalSeedData.users)
                dao.insertPatients(HospitalSeedData.patients)
                dao.insertDocuments(HospitalSeedData.documents)
                dao.insertClinicalNotes(HospitalSeedData.clinicalNotes)
                dao.insertCarePlans(HospitalSeedData.carePlans)
                dao.insertAttendances(HospitalSeedData.attendances)
                dao.insertNotifications(HospitalSeedData.notifications)
            }
        }
    }
}
