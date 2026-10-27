package kr.jinsil.panbyeolgi.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// 디자인 화면(screens/*.dc.html)의 선 아이콘 (24×24, stroke 2.2). 이모지는 쓰지 않는다.
// 색은 Icon의 tint로 정한다.
object LineIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
            "h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

    private fun icon(name: String, vararg paths: String, strokeWidth: Float = 2.2f): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            builder.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    private const val SHIELD = "M12 3l7 3v5c0 4.5-3 8.5-7 10c-4-1.5-7-5.5-7-10V6l7-3z"
    private const val PHONE = "M5 4h4l2 5l-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1z"
    private const val SLASH = "M3 3l18 18"

    val Shield = icon("shield", SHIELD)
    val ShieldCheck = icon("shield-check", SHIELD, "M9 12l2 2l4-4")
    val MessageAlert = icon("message-alert", "M4 5h16v11H9l-5 4z", "M12 8v3.5", "M12 13.8v0.2")
    val Send = icon("send", "M4 12l16-8l-6 16l-3-7z", "M11 13l9-9")
    val ChevronRight = icon("chevron-right", "M9 5l7 7l-7 7", strokeWidth = 2.4f)
    val ArrowLeft = icon("arrow-left", "M19 12H5", "M11 6l-6 6l6 6", strokeWidth = 2.4f)
    val Phone = icon("phone", PHONE)
    val PhoneOff = icon("phone-off", PHONE, SLASH)
    val Bell = icon("bell", "M6 16V11a6 6 0 0 1 12 0v5l2 2H4z", "M10 20a2 2 0 0 0 4 0")
    val Clipboard = icon("clipboard", rect(6f, 4f, 12f, 17f, 2f), "M9 4h6v3H9z")
    val Octagon = icon("octagon-alert", "M8 3h8l5 5v8l-5 5H8l-5-5V8z", "M12 8v5", "M12 16.5v0.5")
    val Triangle = icon("triangle-alert", "M12 4l9 16H3z", "M12 10v4", "M12 17v0.5")
    val CheckCircle = icon("check-circle", circle(12f, 12f, 9f), "M8 12.5l2.5 2.5l5-5.5")
    val LinkOff = icon(
        "link-off",
        "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1",
        "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1",
        "M4 4l16 16",
    )
    val MoneyOff = icon("money-off", rect(3f, 6f, 18f, 12f, 2f), circle(12f, 12f, 2.5f), SLASH)
    val Speaker = icon("speaker", "M4 9h4l5-4v14l-5-4H4z", "M16 9a4 4 0 0 1 0 6", "M18.5 6.5a8 8 0 0 1 0 11")
    val People = icon(
        "people",
        circle(9f, 8f, 3.5f),
        "M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6",
        circle(17f, 9f, 2.5f),
        "M16 14.2c2.8 0.3 5 2.5 5 5.8",
    )
    val Info = icon("info", circle(12f, 12f, 9f), "M12 11v5", "M12 7.5v0.5")
    val Close = icon("close", "M6 6l12 12M18 6L6 18", strokeWidth = 2.4f)
    val Trash = icon("trash", "M4 7h16", "M9 7V4h6v3", "M6 7l1 13h10l1-13", strokeWidth = 2.4f)
}
