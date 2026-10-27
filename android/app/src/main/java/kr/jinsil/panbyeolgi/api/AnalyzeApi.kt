package kr.jinsil.panbyeolgi.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jinsil.panbyeolgi.rules.Category
import kr.jinsil.panbyeolgi.rules.Level
import kr.jinsil.panbyeolgi.rules.Mode
import kr.jinsil.panbyeolgi.rules.Verdict
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 판별 서버 POST {baseUrl}/api/analyze 호출 (AI 판별).
 * 응답은 public/js/rules.js의 buildVerdict 반환값과 같은 모양이다.
 * 서버 주소가 없거나 실패하면 null을 돌려주고, 화면은 기기 안 규칙 판별 결과를 그대로 쓴다.
 */
class AnalyzeApi(private val baseUrl: String) {

    val enabled: Boolean get() = baseUrl.isNotEmpty()

    suspend fun analyze(text: String, mode: Mode): Verdict? {
        if (!enabled) return null
        return withContext(Dispatchers.IO) {
            runCatching { post(text, mode) }.getOrNull()
        }
    }

    private fun post(text: String, mode: Mode): Verdict? {
        val connection = URL("$baseUrl/api/analyze").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            // 서버는 AI가 25초 안에 답하지 않으면 규칙 결과만 돌려준다.
            connection.readTimeout = 35_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("text", text.take(MAX_TEXT)).put("mode", mode.key).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return parseVerdict(response)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        // src/analyze.js의 MAX_TEXT와 같다.
        const val MAX_TEXT = 5000

        fun parseVerdict(json: String): Verdict? = runCatching {
            val o = JSONObject(json)
            val level = Level.fromKey(o.string("level")) ?: return null
            val category = Category.fromKey(o.string("category")) ?: return null
            val headline = o.string("headline").ifBlank { return null }
            Verdict(
                mode = Mode.fromKey(o.string("mode")),
                category = category,
                level = level,
                headline = headline,
                reasons = o.optJSONArray("reasons").strings(),
                advice = o.string("advice"),
                factCheck = o.string("factCheck"),
                links = o.optJSONArray("links").strings(),
                usedAI = o.optBoolean("usedAI", false),
            )
        }.getOrNull()

        private fun JSONObject.string(key: String): String = if (isNull(key)) "" else optString(key, "")

        private fun JSONArray?.strings(): List<String> {
            if (this == null) return emptyList()
            return (0 until length()).mapNotNull { i -> optString(i, "").takeIf { !isNull(i) && it.isNotBlank() } }
        }
    }
}
