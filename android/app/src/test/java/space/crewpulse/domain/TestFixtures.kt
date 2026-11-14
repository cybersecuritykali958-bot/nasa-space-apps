package space.crewpulse.domain

import space.crewpulse.data.model.Band
import space.crewpulse.data.model.ConfidenceConfig
import space.crewpulse.data.model.DeviationRule
import space.crewpulse.data.model.EarlyWarningConfig
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status
import space.crewpulse.data.model.TriageConfig

/** Small hand-made inputs shared by engine tests. */
object TestFixtures {
    val systolic = Indicator(
        id = "systolic_bp", name = "Systolic BP", unit = "mmHg", system = "cardiovascular",
        bands = listOf(
            Band(Status.RED, max = 90.0),
            Band(Status.GREEN, min = 90.0, max = 130.0),
            Band(Status.YELLOW, min = 130.0, max = 140.0),
            Band(Status.RED, min = 140.0),
        ),
        baselineDeviationPct = DeviationRule(10.0, 20.0),
        howToMeasure = "", whySpace = "", actions = emptyMap(), sources = emptyList(),
    )

    val ewConfig = EarlyWarningConfig()

    val triageConfig = TriageConfig(
        responseWindowMin = mapOf(Status.RED to 30.0, Status.YELLOW to 1440.0),
        groundHandlingMin = 10.0,
    )

    val rules = EngineRules(
        version = 1,
        earlyWarning = ewConfig,
        confidence = ConfidenceConfig(),
        triage = triageConfig,
        patterns = emptyList(),
        readiness = emptyList(),
    )

    fun env(id: String, delayOneWay: Double) = Environment(id, id, 0.0, 0.5, delayOneWay, emptyList())

    fun daily(values: List<Double>): List<Sample> =
        values.mapIndexed { i, v -> Sample(i * DAY_MILLIS, v, "manual") }

    /** Deterministic pseudo-noise of about +-2. */
    fun noise(i: Int): Double = listOf(1.5, -2.0, 0.5, 2.0, -1.0, -0.5, 1.0, -1.5, 0.0, 2.5)[i % 10]
}
