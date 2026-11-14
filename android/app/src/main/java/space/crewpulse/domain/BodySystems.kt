package space.crewpulse.domain

/** Body systems shown on the body figure. Ids match `system` in indicators.json and risks.json. */
object BodySystems {
    val labels: Map<String, String> = linkedMapOf(
        "cardiovascular" to "Heart & vessels",
        "metabolic" to "Metabolism",
        "immune" to "Immune system",
        "behavioral" to "Sleep & mind",
        "musculoskeletal" to "Bones & muscles",
        "vision" to "Eyes (SANS)",
        "radiation" to "Radiation",
    )

    fun label(id: String): String = labels[id] ?: id
}
