package kr.jinsil.panbyeolgi.alert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneNumberUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kr.jinsil.panbyeolgi.MainActivity
import kr.jinsil.panbyeolgi.R
import kr.jinsil.panbyeolgi.service.CallRisk
import kr.jinsil.panbyeolgi.service.IncomingMessage
import kr.jinsil.panbyeolgi.service.MessageSource
import kr.jinsil.panbyeolgi.ui.theme.Palette

/** 위험 문자·주의 문자·모르는 전화 알림 */
object Alerts {
    private const val CHANNEL_DANGER = "danger"
    private const val CHANNEL_CAUTION = "caution"
    private const val CHANNEL_CALL = "call"
    const val CALL_NOTIFICATION_ID = 1332

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_DANGER, "사기 위험 문자 경고", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "사기일 가능성이 높은 문자·카톡이 오면 화면 가득 알려요"
                },
                NotificationChannel(CHANNEL_CAUTION, "주의가 필요한 문자", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "의심스러운 부분이 있는 문자·카톡을 알려요"
                },
                NotificationChannel(CHANNEL_CALL, "모르는 전화 경고", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "연락처에 없는 번호로 전화가 오면 알려요"
                },
            ),
        )
    }

    /** 원래 알림 key마다 다른 번호를 쓴다 (같은 대화는 같은 경고를 갱신) */
    private fun idFor(key: String): Int = (key.hashCode() and 0x7FFFFFFF) % 1_000_000 + 10_000

    fun showMessageDanger(
        context: Context,
        source: MessageSource,
        message: IncomingMessage,
        reason: String,
        originalKey: String,
    ) {
        val id = idFor(originalKey)
        val intent = AlertActivity.intent(context, source, message, reason, originalKey, id)
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_DANGER)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(Palette.Danger.toArgb())
            .setContentTitle("방금 온 ${source.topic} 사기일 수 있어요")
            .setContentText(preview(message))
            .setStyle(NotificationCompat.BigTextStyle().bigText("${preview(message)}\n\n$reason"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
        post(context, id, builder)
    }

    fun showMessageCaution(context: Context, source: MessageSource, message: IncomingMessage, reason: String) {
        val id = idFor("caution:" + message.text)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_CHECK_TEXT, message.text)
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_CAUTION)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(Palette.CautionBadge.toArgb())
            .setContentTitle("조심하세요. 의심스러운 ${source.predicate}")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n\n${preview(message)}\n\n눌러서 자세히 확인하세요."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
        post(context, id, builder)
    }

    fun showCallWarning(context: Context, number: String, risk: CallRisk) {
        val intent = CallWarningActivity.intent(context, number, risk)
        val pending = PendingIntent.getActivity(
            context,
            CALL_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_CALL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setColor(Palette.CautionBadge.toArgb())
            .setContentTitle(risk.title)
            .setContentText("${displayNumber(number)} · 저장되지 않은 번호예요")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            // 전화가 끝날 즈음 알림도 사라지게 한다.
            .setTimeoutAfter(90_000)
        post(context, CALL_NOTIFICATION_ID, builder)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun displayNumber(number: String): String =
        if (number.isBlank()) "발신 번호 없음" else PhoneNumberUtils.formatNumber(number, "KR") ?: number

    private fun preview(message: IncomingMessage): String {
        val text = message.text.replace('\n', ' ')
        val body = if (text.length > 120) text.take(120) + "…" else text
        return if (message.sender.isNotBlank()) "${message.sender}: $body" else body
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
