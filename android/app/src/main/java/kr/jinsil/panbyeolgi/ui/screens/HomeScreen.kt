package kr.jinsil.panbyeolgi.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.jinsil.panbyeolgi.rules.Mode
import kr.jinsil.panbyeolgi.ui.components.EmergencyCallButtons
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.Tappable
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type

// docs/design/screens/Main.dc.html
@Composable
fun HomeScreen(
    protectionOn: Boolean,
    hasSharedText: Boolean,
    onChoose: (Mode) -> Unit,
    onSettings: () -> Unit,
) {
    ScreenColumn(
        modifier = Modifier.screenBackground().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        spacing = 20.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LineIcons.ShieldCheck, contentDescription = null, tint = Palette.Surface, modifier = Modifier.size(36.dp))
            }
            Heading("진실판별기", Type.AppTitle, color = Palette.Ink)
        }

        ProtectionStatus(protectionOn, onSettings)

        if (hasSharedText) {
            Text(
                "공유받은 글이 있어요. 아래에서 무엇을 확인할지 눌러 주세요.",
                style = Type.Notice,
                color = Palette.PrimarySoftInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.PrimarySoft)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            )
        }

        Heading("무엇을 확인할까요?", Type.Question, Modifier.padding(top = 8.dp), Palette.Ink)

        ChoiceCard(
            title = "받은 문자·카톡 확인",
            subtitle = "사기 문자인지 알려드려요",
            icon = LineIcons.MessageAlert,
            iconColor = Palette.Danger,
            iconBackground = Palette.DangerSoft,
            onClick = { onChoose(Mode.SCAM) },
        )
        ChoiceCard(
            title = "보내기 전에 확인",
            subtitle = "사실인지 알려드려요",
            icon = LineIcons.Send,
            iconColor = Palette.Primary,
            iconBackground = Palette.PrimarySoft,
            onClick = { onChoose(Mode.SHARE) },
        )

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Heading("이미 돈을 보냈거나 걱정되면", Type.BodyStrong, color = Palette.InkMuted)
            EmergencyCallButtons()
        }

        Tappable(
            onClick = onSettings,
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 60.dp,
        ) {
            Icon(LineIcons.Bell, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(26.dp))
            Text("자동 보호 설정", style = Type.Link, color = Palette.Primary)
        }
    }
}

@Composable
private fun ProtectionStatus(on: Boolean, onSettings: () -> Unit) {
    val background = if (on) Palette.StatusOnBg else Palette.CautionBg
    val ink = if (on) Palette.StatusOnInk else Palette.CautionInk
    val dot = if (on) Palette.StatusOnDot else Palette.CautionBadge
    Tappable(
        onClick = onSettings,
        shape = RoundedCornerShape(16.dp),
        color = background,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
        Text(
            if (on) "자동 보호 켜짐 · 문자를 바로 확인해요" else "자동 보호 꺼짐 · 눌러서 켜 주세요",
            style = Type.BodyStrong,
            color = ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Tappable(
        onClick = onClick,
        shape = shape,
        color = Palette.Surface,
        modifier = Modifier.fillMaxWidth().shadow(8.dp, shape, ambientColor = Palette.Ink.copy(alpha = 0.06f), spotColor = Palette.Ink.copy(alpha = 0.06f)),
        border = BorderStroke(2.dp, Palette.Border),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        role = Role.Button,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(34.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Type.CardTitle.copy(fontSize = 25.sp, lineHeight = 32.sp), color = Palette.Ink)
            Text(subtitle, style = Type.Body.copy(fontSize = 20.sp, lineHeight = 28.sp), color = Palette.InkMuted)
        }
        Icon(LineIcons.ChevronRight, contentDescription = null, tint = Palette.Chevron, modifier = Modifier.size(26.dp))
    }
}
