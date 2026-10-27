package kr.jinsil.panbyeolgi.service

import kr.jinsil.panbyeolgi.data.Settings
import kr.jinsil.panbyeolgi.rules.Level
import kr.jinsil.panbyeolgi.rules.Rules

/** 자동으로 확인하는 앱 (docs/android-plan.md 2절) */
enum class MessageSource(
    val packages: Set<String>,
    val noun: String,
    /** "문자는" / "카톡은" */
    val topic: String,
    /** "문자를" / "카톡을" */
    val objectForm: String,
    /** "문자예요" / "카톡이에요" */
    val predicate: String,
    val deleteHelp: String,
    val openLabel: String,
) {
    SMS(
        packages = setOf("com.samsung.android.messaging", "com.google.android.apps.messaging"),
        noun = "문자",
        topic = "문자는",
        objectForm = "문자를",
        predicate = "문자예요",
        deleteHelp = "문자 앱에서 이 문자를 길게 누른 뒤 \"삭제\"를 눌러 주세요.",
        openLabel = "문자 앱 열기",
    ),
    KAKAO(
        packages = setOf("com.kakao.talk"),
        noun = "카톡",
        topic = "카톡은",
        objectForm = "카톡을",
        predicate = "카톡이에요",
        deleteHelp = "카카오톡 대화방에서 이 메시지를 길게 누른 뒤 \"삭제\"를 눌러 주세요.",
        openLabel = "카카오톡 열기",
    );

    fun enabledIn(settings: Settings): Boolean = if (this == SMS) settings.smsCheck else settings.kakaoCheck

    companion object {
        fun of(packageName: String?): MessageSource? = entries.firstOrNull { packageName in it.packages }
    }
}

data class AutoAlert(val level: Level, val reason: String)

/**
 * 자동 확인은 기기 안 규칙 판별만 쓴다 (개인정보를 서버로 보내지 않음).
 * 받은 글이므로 사기 신호만 본다. 위험이면 전체 화면 경고, 주의면 일반 알림.
 */
object AutoCheck {
    fun evaluate(text: String): AutoAlert? {
        val scam = Rules.analyzeText(text).scam
        if (scam.level == Level.SAFE) return null
        val reason = scam.signals.maxByOrNull { it.weight }?.reason.orEmpty()
        return AutoAlert(scam.level, reason)
    }
}
