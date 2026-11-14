package space.crewpulse.ui.i18n

import androidx.compose.runtime.Composable
import android.annotation.SuppressLint
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import space.crewpulse.R
import space.crewpulse.data.model.Indicator
import space.crewpulse.domain.Alert
import space.crewpulse.domain.AlertKind
import space.crewpulse.domain.Direction
import space.crewpulse.domain.Responder

/**
 * Translated labels for data that comes in English from assets and the engine.
 * Scientific texts (actions, explanations, sources) stay in English, as in the sources.
 */
private val SYSTEMS = mapOf(
    "cardiovascular" to R.string.sys_cardiovascular,
    "metabolic" to R.string.sys_metabolic,
    "immune" to R.string.sys_immune,
    "behavioral" to R.string.sys_behavioral,
    "musculoskeletal" to R.string.sys_musculoskeletal,
    "vision" to R.string.sys_vision,
    "radiation" to R.string.sys_radiation,
)

private val INDICATORS = mapOf(
    "systolic_bp" to R.string.ind_systolic_bp,
    "diastolic_bp" to R.string.ind_diastolic_bp,
    "heart_rate" to R.string.ind_heart_rate,
    "glucose" to R.string.ind_glucose,
    "body_temp" to R.string.ind_body_temp,
    "sleep_hours" to R.string.ind_sleep_hours,
    "mood" to R.string.ind_mood,
    "pvt_mean_rt" to R.string.ind_pvt_mean_rt,
    "cumulative_dose" to R.string.ind_cumulative_dose,
)

private val BADGES = mapOf(
    "7-day streak" to R.string.badge_7,
    "30-day streak" to R.string.badge_30,
    "100-day streak" to R.string.badge_100,
    "Full check-in" to R.string.badge_full,
    "Reaction tester" to R.string.badge_pvt,
)

@Composable
fun systemLabel(id: String): String = SYSTEMS[id]?.let { stringResource(it) } ?: id

@Composable
fun indicatorName(indicator: Indicator): String = indicatorName(indicator.id, indicator.name)

@Composable
fun indicatorName(id: String, fallback: String): String = INDICATORS[id]?.let { stringResource(it) } ?: fallback

@Composable
fun badgeLabel(name: String): String = BADGES[name]?.let { stringResource(it) } ?: name

@Composable
fun responderLabel(r: Responder): String = stringResource(
    when (r) {
        Responder.SELF -> R.string.responder_self
        Responder.CREW_MEDICAL_OFFICER -> R.string.responder_cmo
        Responder.FLIGHT_SURGEON -> R.string.responder_surgeon
        Responder.ONBOARD_PROTOCOL -> R.string.responder_onboard
    },
)

/** Builds the alert sentence from structured fields, so it is translated. */
@Composable
fun alertText(alert: Alert, indicatorName: String): String {
    val up = alert.direction == Direction.UP
    return when (alert.kind) {
        AlertKind.TREND_TO_THRESHOLD -> stringResource(
            if (up) R.string.alert_trend_up else R.string.alert_trend_down,
            indicatorName,
            alert.daysToThreshold ?: 0.0,
        )
        AlertKind.EWMA_SHIFT -> stringResource(
            if (up) R.string.alert_shift_up else R.string.alert_shift_down,
            indicatorName,
            kotlin.math.abs(alert.sigmas ?: 0.0),
        )
        AlertKind.CUSUM_DRIFT -> stringResource(
            if (up) R.string.alert_drift_up else R.string.alert_drift_down,
            indicatorName,
        )
    }
}

private val LEVEL_TITLES = mapOf(
    "Cadet" to R.string.level_cadet,
    "Mission Specialist" to R.string.level_specialist,
    "Flight Engineer" to R.string.level_engineer,
    "Science Officer" to R.string.level_science,
    "Commander" to R.string.level_commander,
)

@Composable
fun levelTitle(title: String): String = LEVEL_TITLES[title]?.let { stringResource(it) } ?: title

/**
 * Triage explanation built from the engine's structured result, so it is translated.
 * The engine's own English sentence (`TriageDecision.reason`) stays available for logs and docs.
 */
@Composable
fun triageReason(report: space.crewpulse.domain.EngineReport): String {
    val t = report.triage
    val attention = report.systems.count {
        it.status == space.crewpulse.data.model.Status.YELLOW || it.status == space.crewpulse.data.model.Status.RED
    }
    return when (t.responder) {
        Responder.ONBOARD_PROTOCOL -> stringResource(R.string.reason_onboard, t.earthResponseMin ?: 0.0, t.responseWindowMin ?: 0.0)
        Responder.FLIGHT_SURGEON -> stringResource(R.string.reason_surgeon, t.earthResponseMin ?: 0.0)
        Responder.CREW_MEDICAL_OFFICER -> stringResource(
            if (report.patterns.isNotEmpty()) R.string.reason_cmo_pattern else R.string.reason_cmo,
            attention,
        )
        Responder.SELF -> when {
            report.systems.isEmpty() -> stringResource(R.string.reason_no_data)
            attention > 0 -> stringResource(R.string.reason_self_yellow)
            report.alerts.isNotEmpty() -> stringResource(R.string.reason_self_alerts, report.alerts.size)
            else -> stringResource(R.string.reason_self_ok)
        }
    }
}

/** One readiness reason in the current language. */
@Composable
fun readinessItemText(item: space.crewpulse.domain.ReadinessItem, state: space.crewpulse.ui.home.HomeUiState): String {
    val ind = item.indicatorId?.let { state.evaluation(it)?.indicator }
    val name = item.indicatorId?.let { id -> ind?.let { indicatorName(it) } ?: indicatorName(id, id) }.orEmpty()
    return when (item.kind) {
        space.crewpulse.domain.ReadinessItemKind.STATUS -> stringResource(
            R.string.ready_status, name, "%.1f".format(item.value ?: 0.0) + " " + (ind?.unit ?: ""),
            space.crewpulse.ui.components.statusWord(item.status ?: space.crewpulse.data.model.Status.UNKNOWN),
        )
        space.crewpulse.domain.ReadinessItemKind.EARLY_WARNING -> stringResource(R.string.ready_warning, name)
        space.crewpulse.domain.ReadinessItemKind.MISSING -> stringResource(R.string.ready_missing, name)
        space.crewpulse.domain.ReadinessItemKind.SPACE_WEATHER -> stringResource(
            if (item.spaceWeather == space.crewpulse.domain.SpaceWeatherLevel.WARNING) R.string.sw_warning else R.string.sw_watch,
        )
        space.crewpulse.domain.ReadinessItemKind.ALL_GREEN -> stringResource(R.string.ready_all_green)
    }
}

/** Minutes as "34 min" or, from two hours on, "24 h". */
@Composable
fun durationText(minutes: Double?): Pair<String, String> = when {
    minutes == null -> "—" to stringResource(R.string.min)
    minutes >= 120 -> "%.0f".format(minutes / 60) to stringResource(R.string.hours)
    else -> "%.0f".format(minutes) to stringResource(R.string.min)
}

private val ENVIRONMENTS = mapOf(
    "iss" to R.string.env_iss,
    "moon_surface" to R.string.env_moon,
    "mars_transit" to R.string.env_mars_transit,
    "mars_surface" to R.string.env_mars_surface,
)

@Composable
fun environmentName(id: String?, fallback: String): String = id?.let { ENVIRONMENTS[it] }?.let { stringResource(it) } ?: fallback

/**
 * Translated scientific text by resource key (e.g. "ind_heart_rate_why"), falling back to the
 * English text from the assets. Keys follow the asset ids, so new content shows in English until
 * it is translated (AssetContractTest lists what is missing).
 */
@SuppressLint("DiscouragedApi") // keys are built from asset ids, so the lookup is dynamic
@Composable
fun contentText(key: String, fallback: String): String {
    val context = LocalContext.current
    val id = remember(key) { context.resources.getIdentifier(key, "string", context.packageName) }
    return if (id != 0) stringResource(id) else fallback
}

@Composable
fun indicatorAction(indicator: Indicator, status: space.crewpulse.data.model.Status): String =
    contentText("ind_${indicator.id}_${status.name.lowercase()}", indicator.actions[status] ?: "—")

@Composable
fun indicatorWhy(indicator: Indicator): String = contentText("ind_${indicator.id}_why", indicator.whySpace)

@Composable
fun indicatorHow(indicator: Indicator): String = contentText("ind_${indicator.id}_how", indicator.howToMeasure)

@Composable
fun riskName(risk: space.crewpulse.data.model.Risk): String = contentText("risk_${risk.id}", risk.name)

@Composable
fun patternName(p: space.crewpulse.domain.PatternMatch): String = contentText("pat_${p.id}_name", p.name)

@Composable
fun patternSummary(p: space.crewpulse.domain.PatternMatch): String = contentText("pat_${p.id}_summary", p.summary)

@Composable
fun patternAdvice(p: space.crewpulse.domain.PatternMatch): String = contentText("pat_${p.id}_advice", p.advice)
