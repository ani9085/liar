package kr.jinsil.panbyeolgi.alert

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.service.IncomingMessage
import kr.jinsil.panbyeolgi.service.MessageSource
import kr.jinsil.panbyeolgi.service.ProtectionListenerService
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.Tappable
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.JinsilTheme
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type
import kr.jinsil.panbyeolgi.util.Actions
import androidx.compose.ui.graphics.Color as ComposeColor

/** 위험 문자가 오면 뜨는 전체 화면 경고 (docs/design/screens/AlertSms.dc.html) */
class AlertActivity : ComponentActivity() {

    private var alert by mutableStateOf<AlertData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        alert = AlertData.from(intent)
        if (alert == null) {
            finish()
            return
        }
        val store = SettingsStore.get(this)
        setContent {
            val settings by store.settings.collectAsState()
            val data = alert ?: return@setContent
            JinsilTheme(textScale = settings.textScale) {
                AlertScreen(
                    data = data,
                    onDelete = {
                        Alerts.cancel(this, data.alertId)
                        ProtectionListenerService.dismiss(data.originalKey)
                    },
                    onOpenApp = {
                        val opened = data.source.packages.any { Actions.openApp(this, it) }
                        if (opened) finish()
                    },
                    onTellFamily = {
                        Alerts.cancel(this, data.alertId)
                        Actions.tellFamily(
                            this,
                            settings.guardianPhone,
                            "[진실판별기] 사기 의심 ${data.source.objectForm} 받았어요. 확인해 주세요.\n\n" +
                                (if (data.sender.isNotBlank()) "보낸 곳: ${data.sender}\n" else "") +
                                "\"${data.text}\"",
                        )
                    },
                    onClose = {
                        Alerts.cancel(this, data.alertId)
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AlertData.from(intent)?.let { alert = it }
    }

    data class AlertData(
        val source: MessageSource,
        val sender: String,
        val text: String,
        val reason: String,
        val originalKey: String,
        val alertId: Int,
    ) {
        companion object {
            fun from(intent: Intent?): AlertData? {
                intent ?: return null
                val source = MessageSource.entries.getOrNull(intent.getIntExtra(EXTRA_SOURCE, -1)) ?: return null
                return AlertData(
                    source = source,
                    sender = intent.getStringExtra(EXTRA_SENDER).orEmpty(),
                    text = intent.getStringExtra(EXTRA_TEXT).orEmpty(),
                    reason = intent.getStringExtra(EXTRA_REASON).orEmpty(),
                    originalKey = intent.getStringExtra(EXTRA_KEY).orEmpty(),
                    alertId = intent.getIntExtra(EXTRA_ALERT_ID, 0),
                )
            }
        }
    }

    companion object {
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_SENDER = "sender"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_KEY = "key"
        private const val EXTRA_ALERT_ID = "alert_id"

        fun intent(
            context: Context,
            source: MessageSource,
            message: IncomingMessage,
            reason: String,
            originalKey: String,
            alertId: Int,
        ): Intent = Intent(context, AlertActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SOURCE, source.ordinal)
            .putExtra(EXTRA_SENDER, message.sender)
            .putExtra(EXTRA_TEXT, message.text.take(1000))
            .putExtra(EXTRA_REASON, reason)
            .putExtra(EXTRA_KEY, originalKey)
            .putExtra(EXTRA_ALERT_ID, alertId)
    }
}

@Composable
private fun AlertScreen(
    data: AlertActivity.AlertData,
    onDelete: () -> Unit,
    onOpenApp: () -> Unit,
    onTellFamily: () -> Unit,
    onClose: () -> Unit,
) {
    var showDeleteHelp by rememberSaveable(data.originalKey) { mutableStateOf(false) }

    ScreenColumn(
        modifier = Modifier.screenBackground(Palette.AlertBg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        spacing = 20.dp,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(999.dp)).background(Palette.AlertChip).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(LineIcons.Shield, contentDescription = null, tint = Palette.AlertMuted, modifier = Modifier.size(22.dp))
            Text("진실판별기 자동 경고", style = Type.SmallStrong, color = Palette.AlertMuted)
        }

        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                Modifier.size(140.dp).clip(CircleShape).background(Palette.Danger.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(112.dp).clip(CircleShape).background(Palette.Danger), contentAlignment = Alignment.Center) {
                    Icon(LineIcons.Octagon, contentDescription = null, tint = Palette.Surface, modifier = Modifier.size(60.dp))
                }
            }
            Heading(
                "방금 온 ${data.source.topic}\n사기일 수 있어요",
                Type.AppTitle.copy(lineHeight = Type.AppTitle.fontSize * 1.3f, textAlign = TextAlign.Center),
                color = Palette.Surface,
            )
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "보낸 곳 · ${data.sender.ifBlank { "모르는 곳" }}",
                style = Type.SmallStrong,
                color = Palette.Danger,
            )
            Text(data.text.take(400) + if (data.text.length > 400) "…" else "", style = Type.Body, color = Palette.Ink)
        }

        Text(
            buildAnnotatedString {
                if (data.reason.isNotBlank()) append(data.reason + "\n")
                append("링크를 누르거나 적힌 번호로 ")
                withStyle(SpanStyle(color = Palette.Surface, fontWeight = FontWeight.Bold)) { append("전화하지 마세요.") }
            },
            style = Type.Body,
            color = Palette.AlertMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        if (showDeleteHelp) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.AlertChip).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Heading("이렇게 지우세요", Type.SectionTitle.copy(fontWeight = FontWeight.Black), color = Palette.Surface)
                Text(data.source.deleteHelp, style = Type.Body, color = Palette.AlertMuted)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!showDeleteHelp) {
                AlertPrimaryButton("이 ${data.source.noun} 지우기", LineIcons.Trash) {
                    onDelete()
                    showDeleteHelp = true
                }
            } else {
                AlertPrimaryButton(data.source.openLabel, null, onOpenApp)
            }
            Tappable(
                onClick = onTellFamily,
                shape = RoundedCornerShape(20.dp),
                color = ComposeColor.Transparent,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, Palette.AlertMuted),
                minHeight = 68.dp,
            ) {
                Icon(LineIcons.People, contentDescription = null, tint = Palette.Surface, modifier = Modifier.size(28.dp))
                Text("가족에게 알리기", style = Type.SecondaryButton, color = Palette.Surface)
            }
            Tappable(
                onClick = onClose,
                shape = RoundedCornerShape(16.dp),
                color = ComposeColor.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (showDeleteHelp) "닫기" else "아는 ${data.source.predicate}, 닫기",
                    style = Type.Body.copy(fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline),
                    color = Palette.AlertMuted,
                )
            }
        }
    }
}

@Composable
private fun AlertPrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Tappable(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Palette.Surface,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 80.dp,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Palette.Danger, modifier = Modifier.size(30.dp))
        Text(text, style = Type.CardTitle, color = Palette.Danger, textAlign = TextAlign.Center)
    }
}
