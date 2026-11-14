package space.crewpulse.data

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.SpaceWeatherFile
import space.crewpulse.data.model.EnvironmentsFile
import space.crewpulse.data.model.IndicatorsFile
import space.crewpulse.data.model.RisksFile
import space.crewpulse.data.model.Status
import space.crewpulse.domain.BodySystems

/** Parses the real bundled JSON so a broken science file fails CI, not the demo. */
class AssetContractTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val assets = File("src/main/assets")

    private inline fun <reified T> load(name: String): T =
        json.decodeFromString(File(assets, name).readText())

    @Test
    fun allAssetsParseAndReferencesResolve() {
        val indicators = load<IndicatorsFile>("indicators.json").indicators
        val risks = load<RisksFile>("risks.json").risks
        val environments = load<EnvironmentsFile>("environments.json").environments
        val mission = load<DemoMission>("demo_mission.json")

        val indicatorIds = indicators.map { it.id }
        assertEquals("duplicate indicator ids", indicatorIds.size, indicatorIds.toSet().size)

        val riskIds = risks.map { it.id }.toSet()
        indicators.forEach { ind ->
            assertTrue("${ind.id}: unknown system ${ind.system}", ind.system in BodySystems.labels)
            assertTrue("${ind.id}: unknown risk ids", riskIds.containsAll(ind.riskIds))
            assertTrue("${ind.id}: needs at least one source", ind.sources.isNotEmpty())
            assertTrue(
                "${ind.id}: missing actions",
                ind.actions.keys.containsAll(listOf(Status.GREEN, Status.YELLOW, Status.RED)),
            )
        }

        val rules = load<EngineRules>("engine_rules.json")
        val conditions = rules.patterns.flatMap { it.all + it.any }.map { it.indicator } +
            rules.readiness.flatMap { r -> r.gates.map { it.indicator } } +
            rules.schedule.intervalDays.keys + rules.gamification.coreIndicators
        assertTrue("engine_rules.json references unknown indicators", indicatorIds.containsAll(conditions))
        assertTrue(rules.patterns.flatMap { it.riskIds }.all { it in riskIds })

        val weather = load<SpaceWeatherFile>("donki_cache.json")
        assertTrue("donki cache should hold real events", weather.events.isNotEmpty())

        assertTrue(mission.synthetic)
        assertTrue("unknown environment", environments.any { it.id == mission.environmentId })
        mission.days.forEach { day ->
            assertTrue("day ${day.day}: unknown indicator", indicatorIds.containsAll(day.readings.keys))
        }
    }
}
