package space.crewpulse.data.db

import androidx.room.Entity

/**
 * Personal pre-flight baseline for one indicator.
 * Demo baselines come from demo_mission.json; for the user they can be set manually,
 * otherwise they are computed from the first check-ins (see BaselineCalculator).
 */
@Entity(tableName = "baselines", primaryKeys = ["profile", "indicatorId"])
data class BaselineEntity(
    /** Profile.name */
    val profile: String,
    val indicatorId: String,
    val value: Double,
    /** "mission" or "manual". */
    val source: String,
)
