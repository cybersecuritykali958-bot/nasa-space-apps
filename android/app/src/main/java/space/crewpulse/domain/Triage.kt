package space.crewpulse.domain

import space.crewpulse.data.model.Environment
import space.crewpulse.data.model.Status
import space.crewpulse.data.model.TriageConfig

/**
 * Decides who should act, taking communication delay into account.
 * Core idea: if Earth cannot answer within the response window, the crew must act on its own.
 */
object Triage {

    fun decide(
        systems: List<SystemStatus>,
        alerts: List<Alert>,
        patterns: List<PatternMatch>,
        environment: Environment?,
        config: TriageConfig,
    ): TriageDecision {
        val worst = StatusEngine.worst(systems.map { it.status })
        val earth = environment?.let { 2 * it.commDelayMinOneWay + config.groundHandlingMin }
        val window = config.responseWindowMin[worst]
        val yellowCount = systems.count { it.status == Status.YELLOW }
        val where = environment?.name ?: "this mission"

        return when (worst) {
            Status.UNKNOWN -> EngineReport.EMPTY.triage
            Status.GREEN -> if (alerts.isEmpty()) {
                TriageDecision(worst, Responder.SELF, "All systems nominal", "Keep the daily routine.", earth, window)
            } else {
                TriageDecision(
                    worst, Responder.SELF, "Nominal, but something is changing",
                    "${alerts.size} early warning(s): values are still in range but trending. Re-measure tomorrow.",
                    earth, window,
                )
            }
            Status.YELLOW -> if (yellowCount >= 2 || patterns.isNotEmpty()) {
                TriageDecision(
                    worst, Responder.CREW_MEDICAL_OFFICER, "Talk to the crew medical officer",
                    "$yellowCount systems need attention" +
                        (if (patterns.isNotEmpty()) " and a pattern was found: ${patterns.joinToString { it.name }}" else "") +
                        ". Not urgent, but should be reviewed today.",
                    earth, window,
                )
            } else {
                TriageDecision(
                    worst, Responder.SELF, "Follow the actions on the yellow indicator",
                    "One system needs attention. Follow the advice and re-measure.", earth, window,
                )
            }
            Status.RED -> if (earth != null && window != null && earth > window) {
                TriageDecision(
                    worst, Responder.ONBOARD_PROTOCOL, "Act now with the onboard protocol",
                    "Earth can answer in ~${"%.0f".format(earth)} min from $where, " +
                        "longer than the ${"%.0f".format(window)}-min response window. " +
                        "The crew medical officer leads; a report is queued for the flight surgeon.",
                    earth, window,
                )
            } else {
                TriageDecision(
                    worst, Responder.FLIGHT_SURGEON, "Contact the flight surgeon now",
                    "Earth can answer in ~${"%.0f".format(earth ?: 0.0)} min, within the response window. " +
                        "Start the onboard actions while waiting.",
                    earth, window,
                )
            }
        }
    }
}
