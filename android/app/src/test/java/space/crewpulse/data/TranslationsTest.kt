package space.crewpulse.data

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import space.crewpulse.data.model.EngineRules
import space.crewpulse.data.model.IndicatorsFile
import space.crewpulse.data.model.RisksFile

/**
 * Every language must have the same UI strings, and every piece of scientific content
 * (indicator advice, explanations, patterns, risks) must be translated.
 */
class TranslationsTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val res = File("src/main/res")
    private val locales = listOf("values", "values-uk", "values-ja", "values-de")

    private fun strings(dir: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val n = nodes.item(i)
            n.attributes.getNamedItem("name").nodeValue to n.textContent
        }
    }

    @Test
    fun allLanguagesHaveTheSameKeysAndPlaceholders() {
        val base = strings("values")
        val placeholder = Regex("""%\d\$[-+#0 ,(]*[\d.]*[sdf]""")
        locales.drop(1).forEach { dir ->
            val other = strings(dir)
            assertTrue("$dir missing: ${base.keys - other.keys}", other.keys.containsAll(base.keys))
            base.forEach { (key, text) ->
                val expected = placeholder.findAll(text).map { it.value }.sorted().toList()
                val actual = placeholder.findAll(other.getValue(key)).map { it.value }.sorted().toList()
                assertTrue("$dir/$key placeholders $actual != $expected", expected == actual)
            }
        }
    }

    @Test
    fun scientificContentIsTranslated() {
        val assets = File("src/main/assets")
        val keys = mutableListOf<String>()
        json.decodeFromString<IndicatorsFile>(File(assets, "indicators.json").readText()).indicators.forEach { i ->
            keys += listOf("how", "why", "green", "yellow", "red").map { "ind_${i.id}_$it" }
        }
        json.decodeFromString<EngineRules>(File(assets, "engine_rules.json").readText()).patterns.forEach { p ->
            keys += listOf("name", "summary", "advice").map { "pat_${p.id}_$it" }
        }
        json.decodeFromString<RisksFile>(File(assets, "risks.json").readText()).risks.forEach { r -> keys += "risk_${r.id}" }

        locales.forEach { dir ->
            val missing = keys - strings(dir).keys
            assertTrue("$dir is missing translations: $missing", missing.isEmpty())
        }
    }
}
