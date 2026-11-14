package space.crewpulse.domain

data class ReminderMessage(val title: String, val text: String)

/** Turns the measurement plan into a notification, or null when nothing is due yet. */
object Reminders {
    fun message(plan: MeasurementPlan, nowMillis: Long): ReminderMessage? {
        val due = (plan.next24h + plan.later).filter { it.dueAtMillis <= nowMillis }
        if (due.isEmpty()) return null
        val minutes = due.sumOf { it.minutes }
        val urgent = due.any { it.priority == TaskPriority.HIGH }
        return ReminderMessage(
            title = if (urgent) "Re-check needed" else "Time for your health check",
            text = "Measure: ${due.joinToString { it.indicatorName }} (~${"%.0f".format(minutes.coerceAtLeast(1.0))} min).",
        )
    }
}
