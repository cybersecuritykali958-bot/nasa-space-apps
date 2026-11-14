package space.crewpulse.domain

import space.crewpulse.data.model.ReadinessRule
import space.crewpulse.data.model.Status

/** GO / CAUTION / NO-GO before a critical operation such as a spacewalk or landing. */
object Readiness {

    fun check(
        rule: ReadinessRule,
        evaluations: Map<String, Evaluation>,
        alerts: List<Alert>,
        spaceWeather: SpaceWeatherStatus? = null,
    ): ReadinessResult {
        var verdict = Verdict.GO
        val reasons = mutableListOf<String>()

        fun raise(v: Verdict, reason: String) {
            if (v > verdict) verdict = v
            reasons += reason
        }

        for (gate in rule.gates) {
            val e = evaluations[gate.indicator]
            if (e == null) {
                raise(Verdict.CAUTION, "No recent ${gate.indicator} measurement.")
                continue
            }
            val sev = StatusEngine.severity(e.status)
            val label = "${e.indicator.name} ${"%.1f".format(e.value)} ${e.indicator.unit} (${e.status.name.lowercase()})"
            when {
                sev >= StatusEngine.severity(gate.noGoAt) -> raise(Verdict.NO_GO, label)
                sev >= StatusEngine.severity(gate.cautionAt) -> raise(Verdict.CAUTION, label)
                e.status == Status.GREEN && alerts.any { it.indicatorId == gate.indicator } ->
                    raise(Verdict.CAUTION, "${e.indicator.name} is in range but has an early warning.")
            }
        }
        if (rule.spaceWeatherGate && spaceWeather != null) {
            when (spaceWeather.level) {
                SpaceWeatherLevel.WARNING -> raise(Verdict.NO_GO, "Space weather: ${spaceWeather.headline}")
                SpaceWeatherLevel.WATCH -> raise(Verdict.CAUTION, "Space weather: ${spaceWeather.headline}")
                SpaceWeatherLevel.QUIET -> Unit
            }
        }
        if (verdict == Verdict.GO) reasons += "All gates green."
        return ReadinessResult(rule.id, rule.name, verdict, reasons, rule.verified)
    }
}
