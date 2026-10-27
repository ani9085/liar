package kr.jinsil.panbyeolgi.service

import kr.jinsil.panbyeolgi.data.Settings
import kr.jinsil.panbyeolgi.rules.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoProtectionTest {

    @Test
    fun `위험 문자는 전체 화면 경고, 이유는 가장 무거운 신호`() {
        val alert = AutoCheck.evaluate("서울중앙지검 수사관입니다. 고객님 명의 계좌가 범죄에 연루되어 안전계좌로 이체하셔야 합니다.")!!
        assertEquals(Level.DANGER, alert.level)
        assertTrue(alert.reason.contains("기관인 척"))
    }

    @Test
    fun `주의 문자는 일반 알림`() {
        assertEquals(Level.CAUTION, AutoCheck.evaluate("본인 아니시면 연락 주세요")?.level)
    }

    @Test
    fun `평범한 문자와 받은 가짜 건강 정보에는 자동 경고를 띄우지 않는다`() {
        assertNull(AutoCheck.evaluate("병원 예약이 내일 오전 10시로 확정되었습니다."))
        assertNull(AutoCheck.evaluate("의사들이 절대 말하지 않는 비밀. 이 차를 마시면 일주일 만에 당뇨가 낫는다고 하네요."))
    }

    @Test
    fun `문자 앱과 카톡만 확인하고 설정을 따른다`() {
        assertEquals(MessageSource.SMS, MessageSource.of("com.samsung.android.messaging"))
        assertEquals(MessageSource.SMS, MessageSource.of("com.google.android.apps.messaging"))
        assertEquals(MessageSource.KAKAO, MessageSource.of("com.kakao.talk"))
        assertNull(MessageSource.of("com.android.chrome"))
        val kakaoOff = Settings(kakaoCheck = false)
        assertTrue(MessageSource.SMS.enabledIn(kakaoOff))
        assertEquals(false, MessageSource.KAKAO.enabledIn(kakaoOff))
    }

    @Test
    fun `070과 해외 번호는 경고를 강하게`() {
        assertEquals(CallRisk.INTERNET_PHONE, CallRisk.classify("070-1234-5678"))
        assertEquals(CallRisk.INTERNET_PHONE, CallRisk.classify("+82 70-1234-5678"))
        assertEquals(CallRisk.FOREIGN, CallRisk.classify("+1 202-555-0100"))
        assertEquals(CallRisk.FOREIGN, CallRisk.classify("00112025550100"))
        assertEquals(CallRisk.UNKNOWN, CallRisk.classify("010-1234-5678"))
        assertEquals(CallRisk.UNKNOWN, CallRisk.classify("+82 10-1234-5678"))
        assertEquals(CallRisk.UNKNOWN, CallRisk.classify("02-123-4567"))
        assertEquals(CallRisk.HIDDEN, CallRisk.classify(""))
        assertEquals(CallRisk.HIDDEN, CallRisk.classify(null))
        assertTrue(CallRisk.FOREIGN.strong && CallRisk.INTERNET_PHONE.strong)
    }
}
