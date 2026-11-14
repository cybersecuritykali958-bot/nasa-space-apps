package space.crewpulse.domain

import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.Status

/** Status of one body system: the worst of its indicators. */
data class SystemStatus(val systemId: String, val status: Status, val evaluations: List<Evaluation>)

object HealthSnapshot {
    /** Evaluates the latest value of every indicator and groups results by body system. */
    fun build(
        indicators: List<Indicator>,
        latest: Map<String, Double>,
        baselines: Map<String, Double>,
    ): List<SystemStatus> {
        val evaluations = indicators.mapNotNull { ind ->
            latest[ind.id]?.let { StatusEngine.evaluate(ind, it, baselines[ind.id]) }
        }
        val order = BodySystems.labels.keys.toList()
        return evaluations.groupBy { it.indicator.system }
            .map { (system, evals) -> SystemStatus(system, StatusEngine.worst(evals.map { it.status }), evals) }
            .sortedBy { order.indexOf(it.systemId).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }
}
