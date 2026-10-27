package kr.jinsil.panbyeolgi.rules

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

// public/js/rules.js로 만든 예문(rules-fixtures.json)과 Kotlin 규칙이 같은 결과를 내는지 확인한다.
// 예문을 다시 만들려면: node android/tools/rules-fixtures.mjs
class RulesParityTest {

    private val fixtures: List<JSONObject> by lazy {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("rules-fixtures.json")) {
            "rules-fixtures.json이 없어요"
        }
        val array = JSONArray(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        List(array.length()) { array.getJSONObject(it) }
    }

    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

    private fun aiOf(o: JSONObject?): AiResult? = o?.let {
        AiResult(
            scamLevel = it.optString("scam_level"),
            misinfoLevel = it.optString("misinfo_level"),
            headline = it.optString("headline"),
            reasons = it.getJSONArray("reasons").strings(),
            advice = it.optString("advice"),
            factCheck = it.optString("fact_check"),
        )
    }

    private fun assertEvaluation(name: String, expected: JSONObject, actual: Evaluation) {
        assertEquals("$name 점수", expected.getInt("score"), actual.score)
        assertEquals("$name 등급", expected.getString("level"), actual.level.key)
        assertEquals("$name 신호", expected.getJSONArray("signals").strings(), actual.signals.map { it.id })
    }

    @Test
    fun `웹과 같은 예문이 들어 있다`() {
        // test/rules.test.js의 사기 7, 허위정보 2, 안전 5
        assertEquals(14, fixtures.count { it.has("expected") })
    }

    @Test
    fun `웹 기준 예문이 같은 등급으로 나온다`() {
        for (f in fixtures.filter { it.has("expected") }) {
            val name = f.getString("name")
            val expected = f.getJSONObject("expected")
            val analysis = Rules.analyzeText(f.getString("text"))
            if (expected.has("scam")) assertEquals(name, expected.getString("scam"), analysis.scam.level.key)
            if (expected.has("misinfo")) assertEquals(name, expected.getString("misinfo"), analysis.misinfo.level.key)
        }
    }

    @Test
    fun `규칙 판별 결과가 rules_js와 같다`() {
        for (f in fixtures) {
            val name = f.getString("name")
            val analysis = Rules.analyzeText(f.getString("text"))
            assertEquals("$name 링크", f.getJSONArray("links").strings(), analysis.links)
            assertEvaluation("$name 사기", f.getJSONObject("scam"), analysis.scam)
            assertEvaluation("$name 허위정보", f.getJSONObject("misinfo"), analysis.misinfo)
        }
    }

    @Test
    fun `화면에 보여줄 판정이 buildVerdict와 같다`() {
        for (f in fixtures) {
            val name = f.getString("name")
            val mode = Mode.fromKey(f.getString("mode"))
            val v = Rules.buildVerdict(mode, Rules.analyzeText(f.getString("text")), aiOf(f.optJSONObject("ai")))
            val expected = f.getJSONObject("verdict")
            assertEquals("$name 종류", expected.getString("category"), v.category.key)
            assertEquals("$name 등급", expected.getString("level"), v.level.key)
            assertEquals("$name 제목", expected.getString("headline"), v.headline)
            assertEquals("$name 이유", expected.getJSONArray("reasons").strings(), v.reasons)
            assertEquals("$name 행동 요령", expected.getString("advice"), v.advice)
            assertEquals("$name 사실 확인", expected.getString("factCheck"), v.factCheck)
            assertEquals("$name 링크", expected.getJSONArray("links").strings(), v.links)
            assertEquals("$name AI 사용", expected.getBoolean("usedAI"), v.usedAI)
        }
    }
}
