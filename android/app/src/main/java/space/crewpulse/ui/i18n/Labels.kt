package space.crewpulse.ui.i18n

import androidx.compose.runtime.Composable
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
