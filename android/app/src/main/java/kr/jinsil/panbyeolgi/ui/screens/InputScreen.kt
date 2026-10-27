package kr.jinsil.panbyeolgi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kr.jinsil.panbyeolgi.rules.Mode
import kr.jinsil.panbyeolgi.ui.components.BackLink
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.PrimaryButton
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.SecondaryButton
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type

// docs/design/screens/Input.dc.html
@Composable
fun InputScreen(
    mode: Mode,
    text: String,
    error: String?,
    onTextChange: (String) -> Unit,
    onError: (String?) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    ScreenColumn(modifier = Modifier.screenBackground().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackLink(onBack)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Heading(if (mode == Mode.SCAM) "받은 문자·카톡 확인" else "보내기 전에 확인", Type.ScreenTitle, color = Palette.Ink)
            Text(
                if (mode == Mode.SCAM) "받은 글을 복사해서\n아래 칸에 붙여넣으세요." else "보내려는 글을 복사해서\n아래 칸에 붙여넣으세요.",
                style = Type.BodyLarge.copy(fontSize = Type.Link.fontSize),
                color = Palette.InkMuted,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("확인할 글", style = Type.BodyStrong, color = Palette.Ink)
            OutlinedTextField(
                value = text,
                onValueChange = {
                    onTextChange(it)
                    if (error != null) onError(null)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                textStyle = Type.BodyLarge.copy(color = Palette.Ink),
                minLines = 7,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.Primary,
                    unfocusedBorderColor = Palette.Primary,
                    focusedContainerColor = Palette.Surface,
                    unfocusedContainerColor = Palette.Surface,
                    cursorColor = Palette.Primary,
                ),
            )
        }

        SecondaryButton(
            text = "복사한 글 붙여넣기",
            icon = LineIcons.Clipboard,
            onClick = {
                val copied = clipboard.getText()?.text?.trim().orEmpty()
                if (copied.isNotEmpty()) {
                    onTextChange(copied)
                    onError(null)
                } else {
                    onError("복사한 글이 없어요. 문자를 길게 누르고 \"복사\"를 먼저 눌러 주세요.")
                }
            },
        )

        if (error != null) {
            Text(
                error,
                style = Type.BodyStrong,
                color = Palette.Danger,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            buildAnnotatedString {
                append("카톡에서 글을 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("길게 누르고") }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("공유 → 진실판별기") }
                append("를 누르면 붙여넣지 않아도 돼요.")
            },
            style = Type.Notice,
            color = Palette.PrimarySoftInk,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.PrimarySoft)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        )

        PrimaryButton("확인하기", onSubmit)
    }
}
