package com.animesh.commutetracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.animesh.commutetracker.data.model.CommuteMode
import com.animesh.commutetracker.data.model.CommuteRecord
import com.animesh.commutetracker.data.model.DiagnosticLog

@Database(entities = [CommuteRecord::class, CommuteMode::class, DiagnosticLog::class], version = 6, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commuteDao(): CommuteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE commute_records ADD COLUMN detectionMethod TEXT NOT NULL DEFAULT 'WIFI'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create commute_modes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `commute_modes` (
                        `modeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `commuteId` INTEGER NOT NULL, 
                        `transportMode` TEXT NOT NULL, 
                        `durationMinutes` INTEGER NOT NULL, 
                        `cost` INTEGER NOT NULL, 
                        FOREIGN KEY(`commuteId`) REFERENCES `commute_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commute_modes_commuteId` ON `commute_modes` (`commuteId`)")

                // 2. Transfer data from commute_records to commute_modes
                db.execSQL("""
                    INSERT INTO commute_modes (commuteId, transportMode, durationMinutes, cost)
                    SELECT id, transportMode, durationMinutes, IFNULL(cost, 0)
                    FROM commute_records
                    WHERE transportMode IS NOT NULL
                """.trimIndent())

                // 3. Recreate commute_records to remove transportMode and cost
                db.execSQL("""
                    CREATE TABLE `commute_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` TEXT NOT NULL, 
                        `direction` TEXT NOT NULL, 
                        `startTimestamp` INTEGER NOT NULL, 
                        `arrivalTimestamp` INTEGER NOT NULL, 
                        `durationMinutes` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `detectionMethod` TEXT NOT NULL
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO commute_records_new (id, date, direction, startTimestamp, arrivalTimestamp, durationMinutes, status, detectionMethod)
                    SELECT id, date, direction, startTimestamp, arrivalTimestamp, durationMinutes, status, detectionMethod 
                    FROM commute_records
                """.trimIndent())
                
                db.execSQL("DROP TABLE commute_records")
                db.execSQL("ALTER TABLE commute_records_new RENAME TO commute_records")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `commute_modes_new` (
                        `modeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `commuteId` INTEGER NOT NULL, 
                        `transportMode` TEXT NOT NULL, 
                        `durationMinutes` INTEGER, 
                        `cost` INTEGER NOT NULL, 
                        FOREIGN KEY(`commuteId`) REFERENCES `commute_records`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO commute_modes_new (modeId, commuteId, transportMode, durationMinutes, cost) SELECT modeId, commuteId, transportMode, durationMinutes, cost FROM commute_modes")
                db.execSQL("DROP TABLE commute_modes")
                db.execSQL("ALTER TABLE commute_modes_new RENAME TO commute_modes")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commute_modes_commuteId` ON `commute_modes` (`commuteId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "commute_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
