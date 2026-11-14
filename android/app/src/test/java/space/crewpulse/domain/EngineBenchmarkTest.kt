package space.crewpulse.domain

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.DemoMission
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.IndicatorsFile

/** Runs the engine self-validation on the real bundled demo mission. */
class EngineBenchmarkTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun text(name: String) = File("src/main/assets", name).readText()

    @Test
    fun replayOnDemoMission() {
        val indicators = json.decodeFromString<IndicatorsFile>(text("indicators.json")).indicators
        val mission = json.decodeFromString<DemoMission>(text("demo_mission.json"))
        val rules = json.decodeFromString<EngineRules>(text("engine_rules.json"))

        val report = EngineBenchmark.run(indicators, mission, rules)
        println(report.summary())
        report.results.forEach { println("  $it") }

        assertTrue("engine should catch at least one event before the threshold", report.earlyCatches > 0)
    }
}
