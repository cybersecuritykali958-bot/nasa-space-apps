package space.crewpulse.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.EnvironmentsFile
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.IndicatorsFile
import space.crewpulse.data.model.Risk
import space.crewpulse.data.model.RisksFile

/** Reads the bundled science data from assets/. Works fully offline. */
class AssetRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun indicators(): List<Indicator> =
        read<IndicatorsFile>("indicators.json").indicators

    suspend fun environments(): List<Environment> =
        read<EnvironmentsFile>("environments.json").environments

    suspend fun risks(): List<Risk> =
        read<RisksFile>("risks.json").risks

    suspend fun demoMission(): DemoMission = read("demo_mission.json")

    suspend fun engineRules(): EngineRules = read("engine_rules.json")

    private suspend inline fun <reified T> read(name: String): T = withContext(Dispatchers.IO) {
        val text = context.assets.open(name).bufferedReader().use { it.readText() }
        json.decodeFromString<T>(text)
    }
}
