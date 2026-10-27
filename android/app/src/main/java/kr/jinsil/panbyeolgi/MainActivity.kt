package kr.jinsil.panbyeolgi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kr.jinsil.panbyeolgi.ui.MainViewModel
import kr.jinsil.panbyeolgi.ui.Screen
import kr.jinsil.panbyeolgi.ui.screens.HomeScreen
import kr.jinsil.panbyeolgi.ui.screens.InputScreen
import kr.jinsil.panbyeolgi.ui.screens.ResultScreen
import kr.jinsil.panbyeolgi.ui.screens.SettingsScreen
import kr.jinsil.panbyeolgi.ui.theme.JinsilTheme
import kr.jinsil.panbyeolgi.util.Actions
import kr.jinsil.panbyeolgi.util.Permissions

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent { JinsilApp(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when {
            // 카톡·문자에서 공유 → 진실판별기
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true -> {
                val shared = listOfNotNull(
                    intent.getStringExtra(Intent.EXTRA_SUBJECT),
                    intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                ).map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n")
                if (shared.isNotEmpty()) viewModel.receiveShared(shared)
            }
            // "주의" 알림을 눌러 들어온 경우
            intent?.hasExtra(EXTRA_CHECK_TEXT) == true -> {
                intent.getStringExtra(EXTRA_CHECK_TEXT)?.takeIf { it.isNotBlank() }?.let(viewModel::checkReceived)
                intent.removeExtra(EXTRA_CHECK_TEXT)
            }
        }
    }

    companion object {
        const val EXTRA_CHECK_TEXT = "kr.jinsil.panbyeolgi.CHECK_TEXT"
    }
}

@Composable
private fun JinsilApp(viewModel: MainViewModel) {
    val settings by viewModel.settingsStore.settings.collectAsState()
    val context = LocalContext.current

    // 설정 앱에서 허용하고 돌아오면 다시 읽는다.
    var access by remember { mutableStateOf(Permissions.read(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = Permissions.read(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    JinsilTheme(textScale = settings.textScale) {
        BackHandler(enabled = viewModel.backStack.size > 1) { viewModel.back() }

        when (val screen = viewModel.screen) {
            Screen.Home -> HomeScreen(
                protectionOn = access.notificationListener && access.postNotifications && (settings.smsCheck || settings.kakaoCheck),
                hasSharedText = viewModel.sharedText != null,
                onChoose = viewModel::chooseMode,
                onSettings = { viewModel.open(Screen.Settings) },
            )
            is Screen.Input -> InputScreen(
                mode = screen.mode,
                text = viewModel.inputText,
                error = viewModel.inputError,
                onTextChange = { viewModel.inputText = it },
                onError = { viewModel.inputError = it },
                onBack = { viewModel.back() },
                onSubmit = { viewModel.submit(screen.mode) },
            )
            Screen.Result -> {
                val result = viewModel.result
                if (result == null) {
                    LaunchedEffect(Unit) { viewModel.home() }
                } else {
                    ResultScreen(
                        state = result,
                        canSpeak = viewModel.speaker.available,
                        onSpeak = viewModel::speak,
                        onAskFamily = { viewModel.familyMessage()?.let { Actions.share(context, it) } },
                        onHome = viewModel::home,
                    )
                }
            }
            Screen.Settings -> SettingsScreen(
                settings = settings,
                access = access,
                store = viewModel.settingsStore,
                onBack = { viewModel.back() },
            )
        }
    }
}
