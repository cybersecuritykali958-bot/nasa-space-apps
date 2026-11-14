package space.crewpulse.domain

import space.crewpulse.data.model.Indicator
import space.crewpulse.data.model.InputConfig

/** Outcome of checking user input before it is stored. */
data class ValidationResult(
    /** Values that passed, ready to save. */
    val accepted: Map<String, Double>,
    /** Indicator id -> human-readable reason. Empty means everything passed. */
    val errors: Map<String, String>,
) {
    val isValid: Boolean get() = errors.isEmpty() && accepted.isNotEmpty()
}

/**
 * Rejects typos and impossible values before they reach the database and the engine.
 * Limits are plausibility bounds from engine_rules.json (`input.limits`), not medical thresholds:
 * a red value is still accepted, a heart rate of 900 is not.
 */
object InputValidator {

    fun validate(values: Map<String, Double>, indicators: List<Indicator>, config: InputConfig): ValidationResult {
        val byId = indicators.associateBy { it.id }
        val accepted = mutableMapOf<String, Double>()
        val errors = mutableMapOf<String, String>()

        if (values.isEmpty()) errors["_"] = "Enter at least one value."

        for ((id, value) in values) {
            val indicator = byId[id]
            val range = config.limits[id]
            when {
                indicator == null -> errors[id] = "Unknown indicator '$id'."
                !value.isFinite() -> errors[id] = "${indicator.name}: enter a number."
                range != null && (value < range.min || value > range.max) ->
                    errors[id] = "${indicator.name} must be between ${fmt(range.min)} and ${fmt(range.max)} ${indicator.unit}."
                else -> accepted[id] = value
            }
        }

        // Cross-check: diastolic pressure cannot be equal to or above systolic.
        val sys = accepted["systolic_bp"]
        val dia = accepted["diastolic_bp"]
        if (sys != null && dia != null && dia >= sys) {
            errors["diastolic_bp"] = "Diastolic pressure must be lower than systolic. Check the order of the values."
            accepted.remove("diastolic_bp")
            accepted.remove("systolic_bp")
        }
        return ValidationResult(accepted, errors)
    }

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
}
