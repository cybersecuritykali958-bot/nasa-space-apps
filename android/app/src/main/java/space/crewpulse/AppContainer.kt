package space.crewpulse

import android.content.Context
import space.crewpulse.data.AssetRepository
import space.crewpulse.data.ManualSessionStore
import space.crewpulse.data.db.AppDatabase
import space.crewpulse.data.db.ReadingDao

/** Manual dependency container. Add new repositories here instead of pulling in a DI framework. */
class AppContainer(context: Context) {
    val manualSession: ManualSessionStore = ManualSessionStore()
    val assets: AssetRepository by lazy { AssetRepository(context.applicationContext) }
    private val database: AppDatabase by lazy { AppDatabase.build(context.applicationContext) }
    val readingDao: ReadingDao get() = database.readingDao()
}
