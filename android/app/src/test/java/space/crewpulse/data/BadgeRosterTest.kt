package space.crewpulse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeRosterTest {
    private val roster = listOf(CrewMember("CP-0001", "Alex Rivera", "commander"))

    @Test
    fun knownBadgeSignsIn() {
        assertEquals("Alex Rivera", findInRoster(" cp-0001 ", roster)?.name)
    }

    @Test
    fun unknownCardIsRefused() {
        assertNull(findInRoster("UID:04A1B2C3", roster))
    }
}
