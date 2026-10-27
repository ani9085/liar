package kr.jinsil.panbyeolgi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// docs/design/README.md의 디자인 토큰

object Palette {
    val Background = Color(0xFFF3F5F9)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5E7EB)
    val ButtonBorder = Color(0xFFD1D5DB)
    val Divider = Color(0xFFEEF0F3)
    val Ink = Color(0xFF111827)
    val InkMuted = Color(0xFF4B5563)
    val InkQuote = Color(0xFF374151)
    val Chevron = Color(0xFF6B7280)
    val SwitchOff = Color(0xFF9CA3AF)

    val Primary = Color(0xFF1E40AF)
    val PrimarySoft = Color(0xFFE8EEFC)
    val PrimarySoftInk = Color(0xFF1E3A8A)

    val Danger = Color(0xFFB42318)
    val DangerSoft = Color(0xFFFDECEA)
    val DangerBorder = Color(0xFFF3C4BF)

    val CautionBg = Color(0xFFFFE9B0)
    val CautionInk = Color(0xFF3D2200)
    val CautionBadge = Color(0xFF5C3300)

    val SafeBg = Color(0xFFD5F0DF)
    val SafeInk = Color(0xFF06331F)
    val SafeBadge = Color(0xFF0B4A2E)

    val StatusOnBg = Color(0xFFE6F4EC)
    val StatusOnInk = Color(0xFF0F5A38)
    val StatusOnDot = Color(0xFF16794C)

    val AlertBg = Color(0xFF2A0E0B)
    val AlertChip = Color(0xFF4A1A15)
    val AlertMuted = Color(0xFFFBD5D0)

    val CallBg = Color(0xFF0F172A)
    val CallMuted = Color(0xFFCBD5E1)
    val CallSecondaryBg = Color(0xFF1E293B)
    val CallSecondaryBorder = Color(0xFF475569)
    val CallSecondaryInk = Color(0xFFE2E8F0)
}

private fun style(size: Float, weight: FontWeight, lineHeight: Float, letterSpacing: TextUnit = TextUnit.Unspecified) =
    TextStyle(fontSize = size.sp, fontWeight = weight, lineHeight = (size * lineHeight).sp, letterSpacing = letterSpacing)

// 글씨는 Noto Sans KR 기준. 안드로이드 기본 한글 글꼴(Noto Sans CJK)을 그대로 쓴다.
object Type {
    val ResultTitle = style(36f, FontWeight.Black, 1.3f, (-0.5).sp)
    val AppTitle = style(34f, FontWeight.Black, 1.2f, (-0.5).sp)
    val ScreenTitle = style(32f, FontWeight.Black, 1.3f, (-0.5).sp)
    val Question = style(28f, FontWeight.Bold, 1.35f)
    val CardTitle = style(27f, FontWeight.Black, 1.3f)
    val PrimaryButton = style(28f, FontWeight.Black, 1.3f)
    val PrimaryButtonSmall = style(26f, FontWeight.Black, 1.3f)
    val Action = style(24f, FontWeight.Black, 1.35f)
    val CallButton = style(24f, FontWeight.Black, 1.3f)
    val SettingTitle = style(24f, FontWeight.Black, 1.35f)
    val SecondaryButton = style(23f, FontWeight.Bold, 1.35f)
    val BodyLarge = style(23f, FontWeight.Normal, 1.55f)
    val Badge = style(22f, FontWeight.Black, 1.3f)
    val SectionTitle = style(22f, FontWeight.Bold, 1.35f)
    val Link = style(22f, FontWeight.Bold, 1.3f)
    val Body = style(21f, FontWeight.Normal, 1.5f)
    val BodyStrong = style(21f, FontWeight.Bold, 1.4f)
    val Notice = style(20f, FontWeight.Normal, 1.5f)
    val Small = style(19f, FontWeight.Normal, 1.4f)
    val SmallStrong = style(19f, FontWeight.Bold, 1.4f)
    val Note = style(18f, FontWeight.Normal, 1.5f)
}

/** 설정의 글씨 크기: 크게 1.0 / 아주 크게 1.2 (휴대폰 글씨 크기 설정 위에 곱한다). */
@Composable
fun JinsilTheme(textScale: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Primary,
            onPrimary = Palette.Surface,
            background = Palette.Background,
            onBackground = Palette.Ink,
            surface = Palette.Surface,
            onSurface = Palette.Ink,
            onSurfaceVariant = Palette.InkMuted,
            outline = Palette.ButtonBorder,
            error = Palette.Danger,
        ),
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * textScale),
            content = content,
        )
    }
}
