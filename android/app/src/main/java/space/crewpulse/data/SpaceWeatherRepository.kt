package space.crewpulse.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import space.crewpulse.data.model.SpaceWeatherEventDto
import space.crewpulse.data.model.SpaceWeatherFile
import space.crewpulse.domain.SpaceWeatherEvent
import space.crewpulse.domain.SpaceWeatherInput
import space.crewpulse.domain.SpaceWeatherType

/**
 * NASA DONKI space weather, offline-first:
 * 1. on start, show the on-device cache, or the cache bundled in assets (real NASA data);
 * 2. refresh() fetches the last 30 days live and saves them to the on-device cache.
 * The demo never depends on the network.
 */
class SpaceWeatherRepository(private val context: Context, private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val cacheFile get() = File(context.filesDir, "donki_cache.json")

    private val _state = MutableStateFlow<SpaceWeatherInput?>(null)
    val state: StateFlow<SpaceWeatherInput?> = _state

    suspend fun load() = withContext(Dispatchers.IO) {
        val cached = runCatching { json.decodeFromString<SpaceWeatherFile>(cacheFile.readText()) }.getOrNull()
        val file = cached ?: json.decodeFromString<SpaceWeatherFile>(
            context.assets.open(BUNDLED).bufferedReader().use { it.readText() },
        )
        _state.value = toInput(file, if (cached != null) "cache" else "bundled")
    }

    /** Fetches recent events from NASA. Returns false (and keeps the current data) on any failure. */
    suspend fun refresh(days: Long = 30): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val end = LocalDate.now(ZoneOffset.UTC)
            val start = end.minusDays(days)
            val live = TYPES.flatMap { type -> fetch(type, start, end) }
            val older = _state.value?.events.orEmpty()
                .filter { it.timeMillis < start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
                .map { it.toDto() }
            val file = SpaceWeatherFile(
                version = 1,
                fetchedAt = Instant.now().toString(),
                events = (older + live).distinctBy { it.id }.sortedBy { it.time },
            )
            cacheFile.writeText(json.encodeToString(SpaceWeatherFile.serializer(), file))
            _state.value = toInput(file, "live")
        }.isSuccess
    }

    private fun fetch(type: String, start: LocalDate, end: LocalDate): List<SpaceWeatherEventDto> {
        val url = URL("https://api.nasa.gov/DONKI/$type?startDate=$start&endDate=$end&api_key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            // DONKI answers 404 for some feeds when there is nothing to report; treat it as empty.
            if (conn.responseCode == 404) return emptyList()
            check(conn.responseCode == 200) { "DONKI $type HTTP ${conn.responseCode}" }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (body.isBlank()) return emptyList()
            return json.parseToJsonElement(body).jsonArray.mapNotNull { normalize(type, it.jsonObject) }
        } finally {
            conn.disconnect()
        }
    }

    private fun toInput(file: SpaceWeatherFile, source: String) = SpaceWeatherInput(
        events = file.events.mapNotNull { it.toDomain() },
        source = source,
        fetchedAtMillis = runCatching { Instant.parse(file.fetchedAt).toEpochMilli() }.getOrNull(),
    )

    companion object {
        const val BUNDLED = "donki_cache.json"
        private val TYPES = listOf("SEP", "CME", "FLR")
        private val DONKI_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneOffset.UTC)

        /** DONKI time (2026-10-16T06:09Z) to ISO instant. */
        internal fun isoTime(donki: String): String = Instant.from(DONKI_TIME.parse(donki)).toString()

        /** Same normalization as science/fetch_donki.py. */
        internal fun normalize(type: String, o: JsonObject): SpaceWeatherEventDto? {
            fun str(key: String) = (o[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
            return when (type) {
                "SEP" -> {
                    val instruments = (o["instruments"] as? JsonArray).orEmpty()
                        .mapNotNull { (it as? JsonObject)?.get("displayName")?.jsonPrimitive?.content }
                    SpaceWeatherEventDto(
                        "SEP", str("sepID") ?: return null, isoTime(str("eventTime") ?: return null),
                        if (instruments.isEmpty()) "Solar energetic particles"
                        else "Solar energetic particles (${instruments.joinToString()})",
                        link = str("link").orEmpty(),
                    )
                }
                "CME" -> {
                    val speed = (o["cmeAnalyses"] as? JsonArray).orEmpty()
                        .mapNotNull { (it as? JsonObject)?.get("speed")?.jsonPrimitive?.doubleOrNull }.maxOrNull()
                    SpaceWeatherEventDto(
                        "CME", str("activityID") ?: return null, isoTime(str("startTime") ?: return null),
                        if (speed != null) "Coronal mass ejection, ${"%.0f".format(speed)} km/s" else "Coronal mass ejection",
                        speedKms = speed, link = str("link").orEmpty(),
                    )
                }
                "FLR" -> {
                    val cls = str("classType")
                    SpaceWeatherEventDto(
                        "FLR", str("flrID") ?: return null, isoTime(str("peakTime") ?: str("beginTime") ?: return null),
                        if (cls != null) "Solar flare $cls" else "Solar flare",
                        flareClass = cls, link = str("link").orEmpty(),
                    )
                }
                else -> null
            }
        }

        internal fun SpaceWeatherEventDto.toDomain(): SpaceWeatherEvent? {
            val t = runCatching { SpaceWeatherType.valueOf(type) }.getOrNull() ?: return null
            val ms = runCatching { Instant.parse(time).toEpochMilli() }.getOrNull() ?: return null
            return SpaceWeatherEvent(t, id, ms, detail, speedKms, flareClass, link)
        }

        internal fun SpaceWeatherEvent.toDto() = SpaceWeatherEventDto(
            type.name, id, Instant.ofEpochMilli(timeMillis).toString(), detail, speedKms, flareClass, link,
        )
    }
}

private fun JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> = this ?: emptyList()
