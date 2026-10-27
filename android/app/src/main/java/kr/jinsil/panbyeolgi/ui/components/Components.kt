package kr.jinsil.panbyeolgi.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type
import kr.jinsil.panbyeolgi.util.Actions

/**
 * 화면 높이보다 짧으면 [content] 안의 Spacer(Modifier.weight(1f))가 남는 공간을 채우고,
 * 글씨가 커져서 길어지면 스크롤된다. ("아주 크게"에서도 글이 잘리지 않게)
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
    spacing: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = viewport).padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )
        }
    }
}

@Composable
fun Heading(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(text, style = style, color = color, modifier = modifier.semantics { heading() })
}

/** 둥근 모서리 + 테두리 + 누르기. 버튼과 카드가 같이 쓴다. */
@Composable
fun Tappable(
    onClick: () -> Unit,
    shape: Shape,
    color: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    minHeight: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    role: Role = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(role = role, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

@Composable
private fun RowScope.ButtonLabel(text: String, icon: ImageVector?, iconSize: Dp, color: Color, style: TextStyle) {
    if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    Text(text, style = style, color = color, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
}

/** 주 버튼: 파란 바탕, 76–80dp, 26–28sp */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minHeight: Dp = 80.dp,
    style: TextStyle = Type.PrimaryButton,
) {
    Tappable(onClick, RoundedCornerShape(22.dp), Palette.Primary, modifier.fillMaxWidth(), minHeight = minHeight) {
        ButtonLabel(text, icon, 30.dp, Palette.Surface, style)
    }
}

/** 보조 버튼: 흰 바탕, 2dp 테두리, 68dp, 23sp */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    minHeight: Dp = 68.dp,
) {
    Tappable(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Palette.Surface,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, Palette.ButtonBorder),
        minHeight = minHeight,
    ) {
        ButtonLabel(text, icon, 28.dp, Palette.Ink, Type.SecondaryButton)
    }
}

/** 경찰 112 · 금감원 1332 */
@Composable
fun EmergencyCallButtons(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tappable(
            onClick = { Actions.dial(context, "112") },
            shape = RoundedCornerShape(20.dp),
            color = Palette.Danger,
            modifier = Modifier.weight(1f),
            minHeight = 76.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        ) {
            ButtonLabel("경찰 112", LineIcons.Phone, 28.dp, Palette.Surface, Type.CallButton)
        }
        Tappable(
            onClick = { Actions.dial(context, "1332") },
            shape = RoundedCornerShape(20.dp),
            color = Palette.Surface,
            modifier = Modifier.weight(1f),
            border = BorderStroke(2.dp, Palette.Danger),
            minHeight = 76.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        ) {
            ButtonLabel("금감원 1332", LineIcons.Phone, 28.dp, Palette.Danger, Type.CallButton)
        }
    }
}

/** 왼쪽 위 "처음으로" */
@Composable
fun BackLink(onClick: () -> Unit, text: String = "처음으로") {
    Tappable(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(LineIcons.ArrowLeft, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(28.dp))
        Text(text, style = Type.Link, color = Palette.Primary)
    }
}

/** 흰 카드 (모서리 24dp, 2dp 테두리) */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = Palette.Surface,
    borderColor: Color? = Palette.Border,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, shape) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** 글머리표 목록 */
@Composable
fun BulletList(items: List<String>, style: TextStyle = Type.Body, color: Color = Palette.Ink, spacing: Dp = 10.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (item in items) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("•", style = style, color = color)
                Text(item, style = style, color = color, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 상태 표시줄 아이콘 색 (어두운 바탕이면 흰 아이콘) */
@Composable
fun StatusBarIcons(darkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(darkBackground) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !darkBackground
        onDispose { if (before != null) controller.isAppearanceLightStatusBars = before }
    }
}

/** 화면 전체 바탕 */
fun Modifier.screenBackground(color: Color = Palette.Background): Modifier = fillMaxSize().background(color)
