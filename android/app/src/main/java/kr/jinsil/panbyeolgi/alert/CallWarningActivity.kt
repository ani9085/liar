package kr.jinsil.panbyeolgi.alert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.service.CallRisk
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.Tappable
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.JinsilTheme
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type

/** 모르는 번호로 전화가 오면 뜨는 경고 (docs/design/screens/CallWarning.dc.html) */
class CallWarningActivity : ComponentActivity() {

    private var number by mutableStateOf("")
    private var risk by mutableStateOf(CallRisk.UNKNOWN)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        read(intent)
        val store = SettingsStore.get(this)
        setContent {
            val settings by store.settings.collectAsState()
            // 전화가 끝났는데 화면이 남아 있지 않도록 잠시 뒤 닫는다.
            LaunchedEffect(number) {
                delay(90_000)
                finish()
            }
            JinsilTheme(textScale = settings.textScale) {
                CallWarningScreen(
                    number = Alerts.displayNumber(number),
                    risk = risk,
                    onDecline = { decline() },
                    onAnswer = { answer() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent?) {
        number = intent?.getStringExtra(EXTRA_NUMBER).orEmpty()
        risk = CallRisk.entries.getOrNull(intent?.getIntExtra(EXTRA_RISK, -1) ?: -1) ?: CallRisk.UNKNOWN
    }

    private fun canControlCalls(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED

    /** 받지 않기: 울리는 전화를 끊는다 (안드로이드 9 이상, "전화 받기·끊기" 허용 필요) */
    @Suppress("DEPRECATION")
    private fun decline() {
        Alerts.cancel(this, Alerts.CALL_NOTIFICATION_ID)
        var ended = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && canControlCalls()) {
            ended = runCatching { getSystemService(TelecomManager::class.java).endCall() }.getOrDefault(false)
        }
        if (!ended) Toast.makeText(this, "빨간 버튼을 눌러 전화를 끊어 주세요", Toast.LENGTH_LONG).show()
        finish()
    }

    /** 그래도 받기: 경고를 닫고 전화를 받는다 */
    @Suppress("DEPRECATION")
    private fun answer() {
        Alerts.cancel(this, Alerts.CALL_NOTIFICATION_ID)
        if (canControlCalls()) {
            runCatching { getSystemService(TelecomManager::class.java).acceptRingingCall() }
        }
        finish()
    }

    companion object {
        private const val EXTRA_NUMBER = "number"
        private const val EXTRA_RISK = "risk"

        fun intent(context: Context, number: String, risk: CallRisk): Intent =
            Intent(context, CallWarningActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_NUMBER, number)
                .putExtra(EXTRA_RISK, risk.ordinal)
    }
}

@Composable
private fun CallWarningScreen(number: String, risk: CallRisk, onDecline: () -> Unit, onAnswer: () -> Unit) {
    ScreenColumn(
        modifier = Modifier.screenBackground(Palette.CallBg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        spacing = 20.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("전화가 오고 있어요", style = Type.Body, color = Palette.CallMuted)
            Text(
                number,
                style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, lineHeight = 50.sp),
                color = Palette.Surface,
                textAlign = TextAlign.Center,
            )
            Text("저장되지 않은 번호", style = Type.Notice, color = Palette.CallMuted)
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.CautionBg).padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(LineIcons.Triangle, contentDescription = null, tint = Palette.CautionInk, modifier = Modifier.size(36.dp))
                Heading(risk.title, Type.Question.copy(fontWeight = FontWeight.Black, lineHeight = 36.sp), color = Palette.CautionInk)
            }
            Text(
                buildAnnotatedString {
                    if (risk.lead.isNotEmpty()) {
                        withStyle(SpanStyle(fontWeight = if (risk.strong) FontWeight.Bold else FontWeight.Normal)) { append(risk.lead) }
                        append(" ")
                    }
                    append("받으셨는데 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("검찰·은행·자녀") }
                    append("라며 돈 이야기를 하면 ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("바로 끊으세요.") }
                },
                style = Type.Body.copy(lineHeight = Type.Body.fontSize * 1.55f),
                color = Palette.CautionInk,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CallChoice(
                text = "받지 않기",
                icon = LineIcons.PhoneOff,
                background = Palette.Danger,
                ink = Palette.Surface,
                border = null,
                style = Type.Action.copy(fontSize = 25.sp),
                modifier = Modifier.weight(1f),
                onClick = onDecline,
            )
            CallChoice(
                text = "그래도 받기",
                icon = LineIcons.Phone,
                background = Palette.CallSecondaryBg,
                ink = Palette.CallSecondaryInk,
                border = BorderStroke(2.dp, Palette.CallSecondaryBorder),
                style = Type.SecondaryButton,
                modifier = Modifier.weight(1f),
                onClick = onAnswer,
            )
        }

        Text(
            "진실판별기가 모르는 번호를 확인했어요",
            style = Type.Note,
            color = Palette.CallMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CallChoice(
    text: String,
    icon: ImageVector,
    background: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    border: BorderStroke?,
    style: TextStyle,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Tappable(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = background,
        modifier = modifier,
        border = border,
        minHeight = 132.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(40.dp))
            Text(text, style = style, color = ink, textAlign = TextAlign.Center)
        }
    }
}
