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
        val items = mutableListOf<ReadinessItem>()

        fun raise(v: Verdict, reason: String, item: ReadinessItem) {
            if (v > verdict) verdict = v
            reasons += reason
            items += item
        }

        for (gate in rule.gates) {
            val e = evaluations[gate.indicator]
            if (e == null) {
                raise(Verdict.CAUTION, "No recent ${gate.indicator} measurement.",
                    ReadinessItem(ReadinessItemKind.MISSING, Verdict.CAUTION, gate.indicator))
                continue
            }
            val sev = StatusEngine.severity(e.status)
            val label = "${e.indicator.name} ${"%.1f".format(e.value)} ${e.indicator.unit} (${e.status.name.lowercase()})"
            when {
                sev >= StatusEngine.severity(gate.noGoAt) -> raise(Verdict.NO_GO, label,
                    ReadinessItem(ReadinessItemKind.STATUS, Verdict.NO_GO, gate.indicator, e.value, e.status))
                sev >= StatusEngine.severity(gate.cautionAt) -> raise(Verdict.CAUTION, label,
                    ReadinessItem(ReadinessItemKind.STATUS, Verdict.CAUTION, gate.indicator, e.value, e.status))
                e.status == Status.GREEN && alerts.any { it.indicatorId == gate.indicator } ->
                    raise(Verdict.CAUTION, "${e.indicator.name} is in range but has an early warning.",
                        ReadinessItem(ReadinessItemKind.EARLY_WARNING, Verdict.CAUTION, gate.indicator, e.value, e.status))
            }
        }
        if (rule.spaceWeatherGate && spaceWeather != null) {
            when (spaceWeather.level) {
                SpaceWeatherLevel.WARNING -> raise(Verdict.NO_GO, "Space weather: ${spaceWeather.headline}",
                    ReadinessItem(ReadinessItemKind.SPACE_WEATHER, Verdict.NO_GO, spaceWeather = spaceWeather.level))
                SpaceWeatherLevel.WATCH -> raise(Verdict.CAUTION, "Space weather: ${spaceWeather.headline}",
                    ReadinessItem(ReadinessItemKind.SPACE_WEATHER, Verdict.CAUTION, spaceWeather = spaceWeather.level))
                SpaceWeatherLevel.QUIET -> Unit
            }
        }
        if (verdict == Verdict.GO) {
            reasons += "All gates green."
            items += ReadinessItem(ReadinessItemKind.ALL_GREEN, Verdict.GO)
        }
        return ReadinessResult(rule.id, rule.name, verdict, reasons, rule.verified, items)
    }
}
