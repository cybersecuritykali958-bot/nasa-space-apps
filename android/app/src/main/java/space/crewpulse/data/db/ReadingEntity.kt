package space.crewpulse.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One measurement entered by the astronaut or produced by a sensor screen. */
@Entity(tableName = "readings", indices = [Index("indicatorId", "timestampMillis")])
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val indicatorId: String,
    val value: Double,
    val timestampMillis: Long,
    /** "manual", "camera", "pvt" or "demo". */
    val source: String,
)
