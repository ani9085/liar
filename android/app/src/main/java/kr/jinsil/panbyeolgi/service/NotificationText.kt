package kr.jinsil.panbyeolgi.service

import android.app.Notification
import androidx.core.app.NotificationCompat

data class IncomingMessage(val sender: String, val text: String)

/**
 * 알림에서 글을 꺼낸다. 카톡은 대화 내용 전체가 아니라 알림 미리보기에 보이는 부분만 읽을 수 있다.
 * MessagingStyle이면 같은 사람이 이어서 보낸 최근 메시지를 함께 본다 (사기 문자가 여러 개로 나뉘어 오는 경우).
 */
object NotificationText {
    private const val MAX_MESSAGES = 5

    fun extract(notification: Notification): IncomingMessage? {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val messages = style?.messages.orEmpty().filter { !it.text.isNullOrBlank() }
        if (style != null && messages.isNotEmpty()) {
            val senderOf = { m: NotificationCompat.MessagingStyle.Message -> m.person?.name?.toString()?.trim().orEmpty() }
            val lastSender = senderOf(messages.last())
            val recent = messages.reversed().takeWhile { senderOf(it) == lastSender }.take(MAX_MESSAGES).reversed()
            val sender = lastSender.ifEmpty { style.conversationTitle?.toString()?.trim().orEmpty() }.ifEmpty { title }
            return IncomingMessage(sender, recent.joinToString("\n") { it.text.toString().trim() })
        }

        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        return IncomingMessage(title, text)
    }
}
