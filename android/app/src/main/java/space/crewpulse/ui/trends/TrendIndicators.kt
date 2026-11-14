package space.crewpulse.ui.trends

const val TRENDS_DEFAULT_ID = "heart_rate"

val TREND_INDICATOR_IDS = listOf(
    "heart_rate",
    "sleep_hours",
    "systolic_bp",
    "glucose",
    "body_temp",
    "mood",
)

fun hasTrendChart(indicatorId: String): Boolean = indicatorId in TREND_INDICATOR_IDS