package space.crewpulse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Who5Test {

    @Test
    fun rawScoreTimesFour() {
        assertEquals(100.0, Who5.percent(listOf(5, 5, 5, 5, 5))!!, 1e-9)
        assertEquals(0.0, Who5.percent(listOf(0, 0, 0, 0, 0))!!, 1e-9)
        // Raw 12 = 48 %, below the cut-off; raw 13 = 52 %, above it (WHO: "raw score below 13").
        assertEquals(48.0, Who5.percent(listOf(3, 3, 2, 2, 2))!!, 1e-9)
        assertEquals(52.0, Who5.percent(listOf(3, 3, 3, 2, 2))!!, 1e-9)
    }

    @Test
    fun incompleteOrOutOfRangeAnswersGiveNoScore() {
        assertNull(Who5.percent(listOf(5, 5, 5, 5, null)))
        assertNull(Who5.percent(listOf(5, 5, 5, 5)))
        assertNull(Who5.percent(listOf(5, 5, 5, 5, 6)))
    }
}
