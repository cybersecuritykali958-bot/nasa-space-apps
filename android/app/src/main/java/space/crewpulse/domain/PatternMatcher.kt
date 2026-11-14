package space.crewpulse.domain

import space.crewpulse.data.model.Condition
import space.crewpulse.data.model.PatternRule

/** Evaluates multi-indicator rules from engine_rules.json and explains each match. */
object PatternMatcher {

    fun match(rules: List<PatternRule>, evaluations: Map<String, Evaluation>, alerts: List<Alert>): List<PatternMatch> =
        rules.mapNotNull { rule ->
            val allOk = rule.all.all { holds(it, evaluations, alerts) }
            val anyHits = rule.any.filter { holds(it, evaluations, alerts) }
            val anyOk = rule.any.isEmpty() || anyHits.size >= rule.minAny
            if (!allOk || !anyOk) return@mapNotNull null

            val evidence = (rule.all + anyHits)
                .map { it.indicator }
                .distinct()
                .mapNotNull { id -> evaluations[id]?.let { describe(it, alerts) } }
            PatternMatch(
                id = rule.id,
                name = rule.name,
                summary = rule.summary,
                evidence = evidence,
                riskIds = rule.riskIds,
                advice = rule.advice,
                sources = rule.sources,
                verified = rule.verified,
            )
        }

    fun holds(c: Condition, evaluations: Map<String, Evaluation>, alerts: List<Alert>): Boolean {
        val e = evaluations[c.indicator] ?: return false
        if (c.minStatus != null && StatusEngine.severity(e.status) < StatusEngine.severity(c.minStatus)) return false
        if (c.statusIn != null && e.status !in c.statusIn) return false
        if (c.minDeviationPct != null && (e.deviationPct == null || e.deviationPct < c.minDeviationPct)) return false
        if (c.maxDeviationPct != null && (e.deviationPct == null || e.deviationPct > c.maxDeviationPct)) return false
        if (c.hasAlert != null && alerts.any { it.indicatorId == c.indicator } != c.hasAlert) return false
        return true
    }

    private fun describe(e: Evaluation, alerts: List<Alert>): String {
        val dev = e.deviationPct?.let { ", ${"%+.0f".format(it)}% vs baseline" }.orEmpty()
        val trend = if (alerts.any { it.indicatorId == e.indicator.id }) ", early warning" else ""
        return "${e.indicator.name}: ${"%.1f".format(e.value)} ${e.indicator.unit} (${e.status.name.lowercase()}$dev$trend)"
    }
}
