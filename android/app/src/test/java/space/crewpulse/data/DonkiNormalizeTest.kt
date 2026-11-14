package space.crewpulse.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import space.crewpulse.data.SpaceWeatherRepository.Companion.toDomain

class DonkiNormalizeTest {

    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject

    @Test
    fun cmeTakesFastestAnalysisSpeed() {
        val dto = SpaceWeatherRepository.normalize(
            "CME",
            obj("""{"activityID":"X-CME-001","startTime":"2026-10-16T06:09Z","cmeAnalyses":[{"speed":800},{"speed":1200.0}],"link":"l"}"""),
        )!!
        assertEquals("2026-10-16T06:09:00Z", dto.time)
        assertEquals(1200.0, dto.speedKms!!, 1e-9)
        assertEquals("CME", dto.toDomain()!!.type.name)
    }

    @Test
    fun sepListsInstrumentsAndSkipsBrokenRecords() {
        val dto = SpaceWeatherRepository.normalize(
            "SEP",
            obj("""{"sepID":"S1","eventTime":"2026-09-05T16:30Z","instruments":[{"displayName":"GOES-18"}]}"""),
        )!!
        assertEquals("Solar energetic particles (GOES-18)", dto.detail)
        assertEquals(null, SpaceWeatherRepository.normalize("SEP", obj("""{"sepID":"S2"}""")))
    }
}
