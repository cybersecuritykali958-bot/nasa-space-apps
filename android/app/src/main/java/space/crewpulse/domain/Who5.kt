package space.crewpulse.domain

/**
 * WHO-5 Well-Being Index scoring (World Health Organization 2024, CC BY-NC-SA 3.0 IGO).
 * Five items about the last two weeks, each 0 ("at no time") to 5 ("all of the time").
 */
object Who5 {
    const val ITEMS = 5
    const val MAX_ITEM_SCORE = 5

    /** Percentage score below this suggests poor well-being and a further assessment (Topp et al. 2015). */
    const val CUT_OFF_PCT = 50.0

    /** Raw score 0-25 multiplied by 4, i.e. 0-100 %. Null until every item is answered with 0-5. */
    fun percent(answers: List<Int?>): Double? {
        if (answers.size != ITEMS || answers.any { it == null || it !in 0..MAX_ITEM_SCORE }) return null
        return answers.sumOf { it!! } * 4.0
    }
}
