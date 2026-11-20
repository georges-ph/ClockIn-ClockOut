package ga.jundbits.clock_in_clock_out.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Profile::class, Attendance::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "clockin_clockout")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        /**
         * Version 1 (app 2.0) stored IDs as integers and joined attendance to the
         * profile list for names. Version 2 stores IDs as text, so leading zeros
         * survive, and copies the name and department into each record.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `Profile_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `department` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `Profile_new` (`id`, `name`, `department`) SELECT CAST(`id` AS TEXT), COALESCE(`name`, ''), COALESCE(`department`, '') FROM `Profile`")
                db.execSQL("DROP TABLE `Profile`")
                db.execSQL("ALTER TABLE `Profile_new` RENAME TO `Profile`")

                db.execSQL("CREATE TABLE `Attendance_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` TEXT NOT NULL, `employeeName` TEXT NOT NULL, `employeeDepartment` TEXT NOT NULL, `clocking` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL(
                    "INSERT INTO `Attendance_new` (`id`, `employeeId`, `employeeName`, `employeeDepartment`, `clocking`, `timestamp`) " +
                        "SELECT a.`id`, CAST(a.`employeeId` AS TEXT), COALESCE(p.`name`, ''), COALESCE(p.`department`, ''), COALESCE(a.`clocking`, 'IN'), a.`timestamp` " +
                        "FROM `Attendance` a LEFT JOIN `Profile` p ON p.`id` = CAST(a.`employeeId` AS TEXT)"
                )
                db.execSQL("DROP TABLE `Attendance`")
                db.execSQL("ALTER TABLE `Attendance_new` RENAME TO `Attendance`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_Attendance_employeeId` ON `Attendance` (`employeeId`)")
            }
        }

        /** Version 3 records how far the phone's clock was moved before each record. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Attendance` ADD COLUMN `clockShift` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
