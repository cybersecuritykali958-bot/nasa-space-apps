package space.crewpulse

import android.content.Context
import space.crewpulse.data.AssetRepository
import space.crewpulse.data.BaselineRepository
import space.crewpulse.data.DemoSeeder
import space.crewpulse.data.ReadingRepository
import space.crewpulse.data.ReportBuilder
import space.crewpulse.data.SettingsStore
import space.crewpulse.data.SpaceWeatherRepository
import space.crewpulse.data.WatchRepository
import space.crewpulse.data.db.AppDatabase

/** Manual dependency container. Add new repositories here instead of pulling in a DI framework. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val assets: AssetRepository by lazy { AssetRepository(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val readings: ReadingRepository by lazy { ReadingRepository(database.readingDao()) }
    val baselines: BaselineRepository by lazy { BaselineRepository(database.baselineDao(), readings) }
    val spaceWeather: SpaceWeatherRepository by lazy { SpaceWeatherRepository(appContext, BuildConfig.NASA_API_KEY) }
    val reportBuilder: ReportBuilder by lazy { ReportBuilder(assets, readings, baselines, settings, spaceWeather) }
    val watch: WatchRepository by lazy { WatchRepository(assets) }
    val demoSeeder: DemoSeeder by lazy { DemoSeeder(assets, database.readingDao(), baselines, settings) }
}
