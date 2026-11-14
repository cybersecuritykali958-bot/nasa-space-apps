package space.crewpulse.domain

import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Indicator
import java.time.ZoneId

/** Everything the engine needs. Pure data, so it is easy to build in tests. */
data class EngineInput(
    val indicators: List<Indicator>,
    /** History per indicator id, any order. */
    val history: Map<String, List<Sample>>,
    val baselines: Map<String, Double>,
    val environment: Environment?,
    val rules: EngineRules,
    val nowMillis: Long,
    /** NASA DONKI events; null when not loaded yet. */
    val spaceWeather: SpaceWeatherInput? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/**
 * Single entry point of the backend logic ("Autonomous Health Reasoning Engine").
 *
 * 1. What is happening?   StatusEngine + EarlyWarning + ConfidenceEstimator
 * 2. What does it mean?   PatternMatcher
 * 3. What to do now?      Triage + Readiness
 *
 * Around it: MeasurementPlanner (what to measure when), Gamification (engagement),
 * SpaceWeather (real NASA DONKI events + career radiation budget).
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
        val spaceWeather = input.spaceWeather?.let {
            SpaceWeather.assess(it.events, input.nowMillis, input.rules.spaceWeather, it.source, it.fetchedAtMillis)
        }
        val readiness = input.rules.readiness.map { Readiness.check(it, evaluations, alerts, spaceWeather) }
        val radiation = SpaceWeather.budget(latest["cumulative_dose"], input.environment, input.rules.spaceWeather)

        val core = EngineReport(systems, alerts, confidence, patterns, triage, readiness)
        val plan = MeasurementPlanner.plan(input.indicators, input.history, core, input.nowMillis, input.rules.schedule, input.zone)
        val game = Gamification.compute(input.history, plan, input.nowMillis, input.zone, input.rules.gamification)
        return core.copy(plan = plan, game = game, spaceWeather = spaceWeather, radiation = radiation)
    }
}
