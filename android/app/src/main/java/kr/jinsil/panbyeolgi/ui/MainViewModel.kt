package kr.jinsil.panbyeolgi.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.jinsil.panbyeolgi.BuildConfig
import kr.jinsil.panbyeolgi.api.AnalyzeApi
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.rules.Level
import kr.jinsil.panbyeolgi.rules.Mode
import kr.jinsil.panbyeolgi.rules.Rules
import kr.jinsil.panbyeolgi.rules.Verdict
import kr.jinsil.panbyeolgi.util.Speaker

sealed interface Screen {
    data object Home : Screen
    data class Input(val mode: Mode) : Screen
    data object Result : Screen
    data object Settings : Screen
}

enum class VerdictSource {
    /** 서버 주소가 없어 휴대폰 안에서만 판별 */
    DEVICE_ONLY,
    /** 서버에 연결하지 못해 휴대폰 안 판별 결과만 */
    OFFLINE,
    /** 서버(규칙 + AI) 판별 */
    SERVER,
}

data class ResultState(
    val text: String,
    val verdict: Verdict,
    val source: VerdictSource,
    /** 서버 판별을 기다리는 중 (기기 안 판별 결과를 먼저 보여 준다) */
    val checking: Boolean,
)

val LEVEL_LABEL = mapOf(Level.DANGER to "위험", Level.CAUTION to "주의", Level.SAFE to "안전해 보여요")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val settingsStore = SettingsStore.get(app)
    private val api = AnalyzeApi(BuildConfig.API_BASE_URL)
    val speaker = Speaker(app)

    val backStack = mutableStateListOf<Screen>(Screen.Home)
    val screen: Screen get() = backStack.last()

    var inputText by mutableStateOf("")
    var inputError by mutableStateOf<String?>(null)

    /** 카톡·문자에서 공유받은 글. 홈에서 무엇을 확인할지 고르면 바로 판별한다. */
    var sharedText by mutableStateOf<String?>(null)
        private set

    var result by mutableStateOf<ResultState?>(null)
        private set

    private var requestId = 0

    fun open(screen: Screen) {
        backStack.add(screen)
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        speaker.stop()
        return true
    }

    fun home() {
        speaker.stop()
        backStack.clear()
        backStack.add(Screen.Home)
    }

    fun receiveShared(text: String) {
        sharedText = text.trim().takeIf { it.isNotEmpty() }
        home()
    }

    fun chooseMode(mode: Mode) {
        val shared = sharedText
        inputError = null
        if (shared != null) {
            // 공유받은 글은 입력 화면을 건너뛰고 바로 확인한다.
            sharedText = null
            inputText = shared
            check(mode, shared)
        } else {
            inputText = ""
            open(Screen.Input(mode))
        }
    }

    fun submit(mode: Mode) {
        val text = inputText.trim()
        if (text.isEmpty()) {
            inputError = "확인할 글을 먼저 붙여넣어 주세요."
            return
        }
        inputError = null
        check(mode, text)
    }

    /** 알림(주의 문자)을 눌러 들어온 경우: 받은 글을 바로 확인한다. */
    fun checkReceived(text: String) {
        inputText = text
        home()
        check(Mode.SCAM, text)
    }

    private fun check(mode: Mode, text: String) {
        val id = ++requestId
        // 먼저 휴대폰 안에서 규칙으로 판별해서 바로 보여 준다.
        val local = Rules.buildVerdict(mode, Rules.analyzeText(text))
        result = ResultState(
            text = text,
            verdict = local,
            source = if (api.enabled) VerdictSource.OFFLINE else VerdictSource.DEVICE_ONLY,
            checking = api.enabled,
        )
        open(Screen.Result)
        if (!api.enabled) return
        viewModelScope.launch {
            val remote = api.analyze(text, mode)
            if (id != requestId) return@launch
            // 서버도 같은 규칙에 AI를 더해 더 위험한 쪽을 따르므로, 서버 결과는 기기 결과보다 덜 위험하지 않다.
            result = result?.copy(
                verdict = remote ?: local,
                source = if (remote != null) VerdictSource.SERVER else VerdictSource.OFFLINE,
                checking = false,
            )
        }
    }

    fun speak() {
        val v = result?.verdict ?: return
        speaker.speak(
            listOf("${LEVEL_LABEL.getValue(v.level)}.", v.headline, v.advice, *v.reasons.toTypedArray(), v.factCheck)
                .filter { it.isNotBlank() }
                .joinToString(" "),
        )
    }

    fun familyMessage(): String? {
        val r = result ?: return null
        return "[진실판별기] 이 글이 괜찮은지 봐줄래요?\n\n\"${r.text}\"\n\n" +
            "판별 결과: ${LEVEL_LABEL.getValue(r.verdict.level)} - ${r.verdict.headline}"
    }

    override fun onCleared() {
        speaker.shutdown()
    }
}
