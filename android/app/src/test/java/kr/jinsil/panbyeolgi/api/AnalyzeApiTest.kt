package kr.jinsil.panbyeolgi.api

import kr.jinsil.panbyeolgi.rules.Category
import kr.jinsil.panbyeolgi.rules.Level
import kr.jinsil.panbyeolgi.rules.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeApiTest {

    @Test
    fun `서버 응답을 판정으로 읽는다`() {
        val v = AnalyzeApi.parseVerdict(
            """
            {"mode":"share","category":"scam","level":"danger","headline":"사기일 가능성이 높아요!",
             "reasons":["단축 주소가 쓰였어요.",""],"advice":"다른 분께 보내지 마세요.","factCheck":"",
             "links":["http://bit.ly/x"],"usedAI":true}
            """.trimIndent(),
        )!!
        assertEquals(Mode.SHARE, v.mode)
        assertEquals(Category.SCAM, v.category)
        assertEquals(Level.DANGER, v.level)
        assertEquals(listOf("단축 주소가 쓰였어요."), v.reasons)
        assertEquals(listOf("http://bit.ly/x"), v.links)
        assertTrue(v.usedAI)
    }

    @Test
    fun `모르는 등급이나 오류 응답은 쓰지 않는다`() {
        assertNull(AnalyzeApi.parseVerdict("""{"error":"확인할 내용을 입력해 주세요."}"""))
        assertNull(AnalyzeApi.parseVerdict("""{"category":"scam","level":"very-bad","headline":"x"}"""))
        assertNull(AnalyzeApi.parseVerdict("""{"category":"scam","level":"safe","headline":null}"""))
        assertNull(AnalyzeApi.parseVerdict("<html>"))
    }

    @Test
    fun `서버 주소가 없으면 꺼진다`() {
        assertEquals(false, AnalyzeApi("").enabled)
    }
}
