package space.crewpulse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReadingEntity::class, BaselineEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingDao(): ReadingDao
    abstract fun baselineDao(): BaselineDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "crewpulse.db")
                // Hackathon build: wipe on schema change instead of writing migrations.
                .fallbackToDestructiveMigration()
                .build()
    }
}
