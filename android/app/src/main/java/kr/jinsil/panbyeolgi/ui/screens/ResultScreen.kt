package kr.jinsil.panbyeolgi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.jinsil.panbyeolgi.rules.Category
import kr.jinsil.panbyeolgi.rules.Level
import kr.jinsil.panbyeolgi.rules.Mode
import kr.jinsil.panbyeolgi.rules.Rules
import kr.jinsil.panbyeolgi.ui.LEVEL_LABEL
import kr.jinsil.panbyeolgi.ui.ResultState
import kr.jinsil.panbyeolgi.ui.VerdictSource
import kr.jinsil.panbyeolgi.ui.components.BulletList
import kr.jinsil.panbyeolgi.ui.components.Card
import kr.jinsil.panbyeolgi.ui.components.EmergencyCallButtons
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.PrimaryButton
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.SecondaryButton
import kr.jinsil.panbyeolgi.ui.components.StatusBarIcons
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type

private class LevelStyle(
    val background: Color,
    val ink: Color,
    val titleInk: Color,
    val badgeBackground: Color,
    val badgeInk: Color,
    val icon: ImageVector,
)

private val LEVEL_STYLE = mapOf(
    Level.DANGER to LevelStyle(Palette.Danger, Palette.Surface, Palette.Surface, Palette.Surface, Palette.Danger, LineIcons.Octagon),
    Level.CAUTION to LevelStyle(Palette.CautionBg, Palette.CautionBadge, Palette.CautionInk, Palette.CautionBadge, Palette.CautionBg, LineIcons.Triangle),
    Level.SAFE to LevelStyle(Palette.SafeBg, Palette.SafeBadge, Palette.SafeInk, Palette.SafeBadge, Palette.SafeBg, LineIcons.CheckCircle),
)

private val SCAM_EXAMPLES = listOf("\"폰이 고장 나서 이 번호로 연락해\"", "\"안전계좌로 옮기세요\"", "\"앱을 설치해 주세요\"")
private val MISINFO_EXAMPLES = listOf("\"언론이 숨기는 진실\"", "\"먹기만 하면 병이 나아요\"", "\"널리 퍼뜨려 주세요\"")

/**
 * 판별 결과. 디자인의 세 화면을 등급과 종류에 따라 고른다.
 * - 사기 위험: ResultScam.dc.html (행동 지시 3개 + 112·1332)
 * - 허위정보 주의/위험: ResultShare.dc.html (보내려던 글 + 실제로는요)
 * - 안전: ResultSafe.dc.html (확인한 글 + 주의할 사기 문구)
 * - 사기 주의: 같은 틀에 행동 요령을 글로 보여 준다.
 */
@Composable
fun ResultScreen(
    state: ResultState,
    canSpeak: Boolean,
    onSpeak: () -> Unit,
    onAskFamily: () -> Unit,
    onHome: () -> Unit,
) {
    val v = state.verdict
    val style = LEVEL_STYLE.getValue(v.level)
    val scamDanger = v.category == Category.SCAM && v.level == Level.DANGER
    val misinfo = v.category == Category.MISINFO && v.level != Level.SAFE
    val safe = v.level == Level.SAFE
    val sharing = v.mode == Mode.SHARE

    StatusBarIcons(darkBackground = v.level == Level.DANGER)

    ScreenColumn(
        modifier = Modifier.screenBackground().windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(0.dp),
        spacing = 0.dp,
    ) {
        // 결과 상단: 색 + 아이콘 + 글자로 함께 구분한다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(style.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(style.icon, contentDescription = null, tint = style.ink, modifier = Modifier.size(48.dp))
                Text(
                    LEVEL_LABEL.getValue(v.level),
                    style = Type.Badge,
                    color = style.badgeInk,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(style.badgeBackground)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Heading(v.headline, Type.ResultTitle, color = style.titleInk)
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.checking) CheckingRow()

            if (!scamDanger && (sharing || safe || misinfo)) {
                OriginalText(if (sharing) "보내려던 글" else "확인한 글", state.text)
            }

            when {
                scamDanger -> DangerActions(advice = v.advice.takeIf { it != Rules.defaultAdvice(Category.SCAM, Level.DANGER) })
                misinfo -> Card(color = Palette.PrimarySoft, borderColor = null, spacing = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(LineIcons.Info, contentDescription = null, tint = Palette.PrimarySoftInk, modifier = Modifier.size(26.dp))
                        Heading("실제로는요", Type.Badge, color = Palette.PrimarySoftInk)
                    }
                    Text(v.factCheck.ifBlank { v.advice }, style = Type.BodyLarge.copy(fontSize = Type.Badge.fontSize), color = Palette.Ink)
                }
                safe -> Card(spacing = 10.dp) {
                    Heading("그래도 기억하세요", Type.SectionTitle, color = Palette.InkMuted)
                    Text(v.advice, style = Type.SecondaryButton.copy(lineHeight = Type.BodyLarge.lineHeight), color = Palette.Ink)
                }
                else -> Card(borderColor = Palette.CautionBg, spacing = 10.dp) {
                    Heading("이렇게 하세요", Type.SectionTitle, color = Palette.InkMuted)
                    Text(v.advice, style = Type.SecondaryButton.copy(lineHeight = Type.BodyLarge.lineHeight), color = Palette.Ink)
                }
            }

            if (scamDanger) EmergencyCallButtons()

            if (!safe && v.reasons.isNotEmpty()) {
                Card {
                    Heading("이렇게 판단했어요", Type.SectionTitle, color = Palette.InkMuted)
                    BulletList(v.reasons)
                }
            }

            if (safe) {
                Card(color = Palette.PrimarySoft, borderColor = null) {
                    Heading(
                        if (sharing) "이런 말이 나오면 가짜일 수 있어요" else "이런 말이 나오면 사기예요",
                        Type.Badge,
                        color = Palette.PrimarySoftInk,
                    )
                    BulletList(if (sharing) MISINFO_EXAMPLES else SCAM_EXAMPLES, spacing = 8.dp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    sharing && !safe -> {
                        PrimaryButton("보내지 않을게요", onHome, icon = LineIcons.Close, style = Type.CardTitle)
                        SecondaryButton("가족에게 물어보기", onAskFamily, icon = LineIcons.People)
                        if (canSpeak) SecondaryButton("소리로 듣기", onSpeak, icon = LineIcons.Speaker)
                    }
                    safe -> {
                        if (canSpeak) SecondaryButton("소리로 듣기", onSpeak, icon = LineIcons.Speaker)
                        PrimaryButton("처음으로", onHome, style = Type.CardTitle)
                    }
                    else -> {
                        if (canSpeak) SecondaryButton("소리로 듣기", onSpeak, icon = LineIcons.Speaker)
                        SecondaryButton("가족에게 물어보기", onAskFamily, icon = LineIcons.People)
                        PrimaryButton("처음으로", onHome, minHeight = 76.dp, style = Type.PrimaryButtonSmall)
                    }
                }
            }

            Text(
                note(state),
                style = Type.Note,
                color = Palette.InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun note(state: ResultState): String {
    if (state.checking) return "판단이 틀릴 수 있어요. 돈과 관련된 일은 꼭 가족과 상의하세요."
    val ending = if (state.verdict.category == Category.MISINFO) {
        "뉴스나 공식 기관에서 같은 내용을 찾아보세요."
    } else {
        "돈과 관련된 일은 꼭 가족과 상의하세요."
    }
    return when (state.source) {
        VerdictSource.OFFLINE -> "서버에 연결하지 못해 휴대폰 안에서 기본 검사만 했어요. 판단이 틀릴 수 있으니 $ending"
        VerdictSource.DEVICE_ONLY -> "휴대폰 안에서 기본 검사를 했어요. 판단이 틀릴 수 있으니 $ending"
        VerdictSource.SERVER -> if (state.verdict.usedAI) "AI의 판단은 틀릴 수 있어요. $ending" else "기본 검사 결과예요. 판단이 틀릴 수 있으니 $ending"
    }
}

@Composable
private fun CheckingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.PrimarySoft)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = Palette.Primary, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
        Text("더 꼼꼼히 살펴보고 있어요…", style = Type.BodyStrong, color = Palette.PrimarySoftInk)
    }
}

@Composable
private fun OriginalText(title: String, text: String) {
    val dashColor = Palette.ButtonBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(20.dp.toPx() - stroke / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Heading(title, Type.SmallStrong, color = Palette.InkMuted)
        Text("\"${text.take(600)}${if (text.length > 600) "…" else ""}\"", style = Type.Body, color = Palette.InkQuote)
    }
}

/** 사기 위험일 때 행동 지시 3개 (아이콘 + 굵은 글씨) */
@Composable
private fun DangerActions(advice: String?) {
    Card(borderColor = Palette.DangerBorder, spacing = 14.dp) {
        Heading("이렇게 하세요", Type.SectionTitle, color = Palette.InkMuted)
        ActionItem(LineIcons.LinkOff, "링크를 누르지 마세요")
        ActionItem(LineIcons.PhoneOff, "답장·전화하지 마세요")
        ActionItem(LineIcons.MoneyOff, "돈·상품권을 보내지 마세요")
        if (advice != null) Text(advice, style = Type.Body, color = Palette.Ink)
    }
}

@Composable
private fun ActionItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Palette.DangerSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Danger, modifier = Modifier.size(28.dp))
        }
        Text(text, style = Type.Action, color = Palette.Ink, modifier = Modifier.weight(1f))
    }
}
