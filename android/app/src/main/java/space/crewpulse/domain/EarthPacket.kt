package space.crewpulse.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import space.crewpulse.data.model.Status

/**
 * Compact plain-text health report for the flight surgeon on Earth. It is always in English and plain ASCII,
 * so it survives any downlink channel, and it only lists what is off-nominal to keep it short.
 */
object EarthPacket {

    data class Timing(
        /** One-way signal delay, minutes. */
        val oneWayMin: Double,
        /** Round trip plus ground handling, minutes (from triage); null if unknown. */
        val earliestReplyMin: Double?,
    )

    fun timing(report: EngineReport, oneWayMin: Double?): Timing? =
        oneWayMin?.let { Timing(it, report.triage.earthResponseMin) }

    fun build(
        report: EngineReport,
        crewName: String?,
        badgeId: String?,
        missionDay: Int,
        environmentName: String,
        nowMillis: Long,
    ): String = buildString {
        val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.US)
            .format(Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC))
        val evaluations = report.systems.flatMap { it.evaluations }
        val offNominal = evaluations.filter { it.status == Status.YELLOW || it.status == Status.RED }
            .sortedByDescending { it.status == Status.RED }
        val overall = StatusEngine.worst(report.systems.map { it.status })

        appendLine("CREWPULSE HEALTH PACKET")
        appendLine("Crew: ${crewName ?: "unknown"}${badgeId?.let { " ($it)" }.orEmpty()}")
        appendLine("Mission day $missionDay, ${environmentName.ifBlank { "unknown environment" }}")
        appendLine("Generated $stamp")
        appendLine("Overall: ${overall.name}")
        appendLine("Triage: ${report.triage.headline}. ${report.triage.reason}")
        appendLine("Off-nominal (${offNominal.size}):")
        if (offNominal.isEmpty()) appendLine("- none")
        offNominal.forEach { e ->
            val dev = e.deviationPct?.let { ", %+.0f%% vs baseline".format(Locale.US, it) }.orEmpty()
            appendLine("- ${e.indicator.name}: ${"%.1f".format(Locale.US, e.value)} ${e.indicator.unit} ${e.status.name}$dev")
        }
        // One line per indicator and direction, even when several detectors (EWMA, CUSUM) agree.
        val trends = report.alerts.groupBy { it.indicatorId to it.direction }
        appendLine("Early warnings (${trends.size}):")
        if (trends.isEmpty()) appendLine("- none")
        trends.forEach { (key, alerts) ->
            val name = evaluations.firstOrNull { it.indicator.id == key.first }?.indicator?.name ?: key.first
            val eta = alerts.mapNotNull { it.daysToThreshold }.minOrNull()
                ?.let { ", threshold in ~%.0f days".format(Locale.US, it) }.orEmpty()
            appendLine("- $name: trend ${key.second.name}$eta")
        }
        if (report.patterns.isNotEmpty()) {
            appendLine("Patterns: ${report.patterns.joinToString("; ") { it.name }}")
        }
        report.spaceWeather?.let { appendLine("Space weather: ${it.level.name}, ${it.headline}") }
        append("Measured ${evaluations.size} indicators; values not listed are nominal.")
    }
}
