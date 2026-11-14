package space.crewpulse.domain

import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Indicator

/** Everything the engine needs. Pure data, so it is easy to build in tests. */
data class EngineInput(
    val indicators: List<Indicator>,
    /** History per indicator id, any order. */
    val history: Map<String, List<Sample>>,
    val baselines: Map<String, Double>,
    val environment: Environment?,
    val rules: EngineRules,
    val nowMillis: Long,
)

/**
 * Single entry point of the backend logic ("Autonomous Health Reasoning Engine").
 *
 * 1. What is happening?   StatusEngine + EarlyWarning + ConfidenceEstimator
 * 2. What does it mean?   PatternMatcher
 * 3. What to do now?      Triage + Readiness
 */
object HealthEngine {

    fun analyze(input: EngineInput): EngineReport {
        val latest = input.history.mapNotNull { (id, list) ->
            list.maxByOrNull { it.timestampMillis }?.let { id to it.value }
        }.toMap()
        if (latest.isEmpty()) return EngineReport.EMPTY

        val systems = HealthSnapshot.build(input.indicators, latest, input.baselines)
        val evaluations = systems.flatMap { it.evaluations }.associateBy { it.indicator.id }

        val alerts = input.indicators.flatMap { ind ->
            EarlyWarning.analyze(ind, input.history[ind.id].orEmpty(), input.baselines[ind.id], input.rules.earlyWarning)
        }
        val confidence = input.indicators
            .filter { input.history[it.id].orEmpty().isNotEmpty() }
            .associate { ind ->
                ind.id to ConfidenceEstimator.estimate(
                    history = input.history.getValue(ind.id),
                    hasBaseline = input.baselines.containsKey(ind.id),
                    nowMillis = input.nowMillis,
                    config = input.rules.confidence,
                )
            }
        val patterns = PatternMatcher.match(input.rules.patterns, evaluations, alerts)
        val triage = Triage.decide(systems, alerts, patterns, input.environment, input.rules.triage)
        val readiness = input.rules.readiness.map { Readiness.check(it, evaluations, alerts) }

        return EngineReport(systems, alerts, confidence, patterns, triage, readiness)
    }
}
