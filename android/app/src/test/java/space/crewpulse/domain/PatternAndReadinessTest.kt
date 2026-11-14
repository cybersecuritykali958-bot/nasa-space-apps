package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.Band
import space.crewpulse.data.model.Condition
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.PatternRule
import space.crewpulse.data.model.ReadinessGate
import space.crewpulse.data.model.ReadinessRule
import space.crewpulse.data.model.Status

class PatternAndReadinessTest {

    private fun ind(id: String) = Indicator(
        id = id, name = id, unit = "", system = "behavioral",
        bands = listOf(Band(Status.GREEN, max = 10.0), Band(Status.YELLOW, min = 10.0, max = 20.0), Band(Status.RED, min = 20.0)),
        howToMeasure = "", whySpace = "", actions = emptyMap(), sources = emptyList(),
    )

    private fun eval(id: String, value: Double, deviation: Double? = null): Evaluation {
        val s = StatusEngine.byRange(ind(id), value)
        return Evaluation(ind(id), value, s, s, null, deviation)
    }

    private fun map(vararg e: Evaluation) = e.associateBy { it.indicator.id }

    private val discordance = PatternRule(
        id = "discordance", name = "Discordance", summary = "",
        all = listOf(Condition("mood", statusIn = listOf(Status.GREEN))),
        any = listOf(Condition("pvt", minDeviationPct = 15.0), Condition("sleep", minStatus = Status.YELLOW)),
        minAny = 2, advice = "",
    )

    @Test
    fun discordanceNeedsGoodMoodAndTwoObjectiveSigns() {
        val hit = PatternMatcher.match(listOf(discordance), map(eval("mood", 1.0), eval("pvt", 5.0, 25.0), eval("sleep", 15.0)), emptyList())
        assertEquals(3, hit.single().evidence.size)

        assertTrue(PatternMatcher.match(listOf(discordance), map(eval("mood", 1.0), eval("pvt", 5.0, 25.0), eval("sleep", 1.0)), emptyList()).isEmpty())
        assertTrue(PatternMatcher.match(listOf(discordance), map(eval("mood", 15.0), eval("pvt", 5.0, 25.0), eval("sleep", 15.0)), emptyList()).isEmpty())
    }

    @Test
    fun readinessGivesNoGoOnRedAndCautionOnMissingData() {
        val rule = ReadinessRule("eva", "EVA", listOf(ReadinessGate("sleep"), ReadinessGate("pvt")))
        assertEquals(Verdict.NO_GO, Readiness.check(rule, map(eval("sleep", 25.0), eval("pvt", 1.0)), emptyList()).verdict)
        assertEquals(Verdict.CAUTION, Readiness.check(rule, map(eval("sleep", 1.0)), emptyList()).verdict)
        assertEquals(Verdict.GO, Readiness.check(rule, map(eval("sleep", 1.0), eval("pvt", 1.0)), emptyList()).verdict)
    }

    @Test
    fun confidenceDropsForStaleAndEstimatedReadings() {
        val cfg = TestFixtures.rules.confidence
        val day = 24L * 60 * 60 * 1000
        val fresh = List(5) { Sample(it * day, 1.0, "manual") }
        assertEquals(ConfidenceLevel.HIGH, ConfidenceEstimator.estimate(fresh, true, 4 * day, cfg).level)
        assertEquals(ConfidenceLevel.LOW, ConfidenceEstimator.estimate(fresh, true, 10 * day, cfg).level)
        val camera = fresh + Sample(4 * day + 1, 1.0, "camera")
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceEstimator.estimate(camera, true, 4 * day + 1, cfg).level)
    }
}
