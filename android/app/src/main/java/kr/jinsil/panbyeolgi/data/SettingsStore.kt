package kr.jinsil.panbyeolgi.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val smsCheck: Boolean = true,
    val kakaoCheck: Boolean = true,
    // 전화 경고는 권한이 많이 필요해서 기본으로 꺼 둔다.
    val callWarning: Boolean = false,
    val guardianName: String = "",
    val guardianPhone: String = "",
    val extraLargeText: Boolean = false,
) {
    /** 크게 1.0 / 아주 크게 1.2 */
    val textScale: Float get() = if (extraLargeText) 1.2f else 1f
}

/**
 * 설정 저장소. 알림 확인 서비스·전화 확인 서비스가 기다리지 않고 바로 읽을 수 있게
 * SharedPreferences에 저장하고 같은 프로세스 안에서는 하나의 StateFlow로 나눠 쓴다.
 */
class SettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<Settings> = state.asStateFlow()
    val current: Settings get() = state.value

    private fun read() = Settings(
        smsCheck = prefs.getBoolean(SMS, true),
        kakaoCheck = prefs.getBoolean(KAKAO, true),
        callWarning = prefs.getBoolean(CALL, false),
        guardianName = prefs.getString(GUARDIAN_NAME, "").orEmpty(),
        guardianPhone = prefs.getString(GUARDIAN_PHONE, "").orEmpty(),
        extraLargeText = prefs.getBoolean(EXTRA_LARGE, false),
    )

    @Synchronized
    fun update(transform: (Settings) -> Settings) {
        val next = transform(state.value)
        prefs.edit()
            .putBoolean(SMS, next.smsCheck)
            .putBoolean(KAKAO, next.kakaoCheck)
            .putBoolean(CALL, next.callWarning)
            .putString(GUARDIAN_NAME, next.guardianName)
            .putString(GUARDIAN_PHONE, next.guardianPhone)
            .putBoolean(EXTRA_LARGE, next.extraLargeText)
            .apply()
        state.value = next
    }

    companion object {
        private const val SMS = "sms_check"
        private const val KAKAO = "kakao_check"
        private const val CALL = "call_warning"
        private const val GUARDIAN_NAME = "guardian_name"
        private const val GUARDIAN_PHONE = "guardian_phone"
        private const val EXTRA_LARGE = "extra_large_text"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
