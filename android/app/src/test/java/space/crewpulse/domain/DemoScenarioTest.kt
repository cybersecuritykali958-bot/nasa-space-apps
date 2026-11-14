package space.crewpulse.domain

import java.io.File
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.SpaceWeatherRepository.Companion.toDomain
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.EnvironmentsFile
import space.crewpulse.data.model.IndicatorsFile
import space.crewpulse.data.model.SpaceWeatherFile

/**
 * End-to-end: the full engine on the bundled demo astronaut, exactly as the Home screen gets it.
 * Prints the report so it can be pasted into docs and checked by eye.
 */
class DemoScenarioTest {

    private val json = Json { ignoreUnknownKeys = true }
    private inline fun <reified T> load(name: String): T = json.decodeFromString(File("src/main/assets", name).readText())

    @Test
    fun demoAstronautOnDay180() {
        val indicators = load<IndicatorsFile>("indicators.json").indicators
        val mission = load<DemoMission>("demo_mission.json")
        val rules = load<EngineRules>("engine_rules.json")
        val env = load<EnvironmentsFile>("environments.json").environments.first { it.id == mission.environmentId }
        val weather = load<SpaceWeatherFile>("donki_cache.json")

        val day = 24L * 60 * 60 * 1000
        val history = indicators.associate { ind ->
            ind.id to mission.days.mapNotNull { d -> d.readings[ind.id]?.let { Sample(d.day * day, it, "demo") } }
        }
        val now = mission.days.maxOf { it.day } * day
        val report = HealthEngine.analyze(
            EngineInput(
                indicators, history, mission.baseline, env, rules, now,
                SpaceWeatherInput(weather.events.mapNotNull { it.toDomain() }, "bundled", null), ZoneOffset.UTC,
            ),
        )

        println("SYSTEMS: " + report.systems.joinToString { "${it.systemId}=${it.status}" })
        report.alerts.forEach { println("ALERT: ${it.kind} ${it.message}") }
        report.patterns.forEach { println("PATTERN: ${it.name} <- ${it.evidence}") }
        println("TRIAGE: ${report.triage.responder} | ${report.triage.headline} | ${report.triage.reason}")
        report.readiness.forEach { println("READINESS: ${it.operationName} ${it.verdict} ${it.reasons}") }
        println("PLAN: next24h=${report.plan?.next24h?.map { it.indicatorId + ":" + it.reason }} saved=${report.plan?.minutesSaved} min")
        println("GAME: ${report.game}")
        println("RADIATION: ${report.radiation}")
        println("WEATHER (as of synthetic day 180): ${report.spaceWeather?.level} ${report.spaceWeather?.headline}")

        assertTrue(report.systems.isNotEmpty())
        assertTrue("demo should show at least one early warning", report.alerts.isNotEmpty())
    }
}
