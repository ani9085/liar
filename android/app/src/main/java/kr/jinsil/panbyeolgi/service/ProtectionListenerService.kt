package kr.jinsil.panbyeolgi.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kr.jinsil.panbyeolgi.alert.Alerts
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.rules.Level
import java.lang.ref.WeakReference

/**
 * 문자·카톡 자동 확인. 사용자가 설정에서 "알림 접근"을 허용하면 동작한다.
 * READ_SMS / RECEIVE_SMS 권한은 쓰지 않는다 (Play 스토어 정책상 기본 문자 앱만 쓸 수 있음).
 */
class ProtectionListenerService : NotificationListenerService() {

    // 같은 대화 알림이 갱신될 때마다 다시 경고하지 않도록, 알림별로 이미 알린 등급을 기억한다.
    private val alerted = LinkedHashMap<String, Level>()

    override fun onListenerConnected() {
        active = WeakReference(this)
    }

    override fun onListenerDisconnected() {
        if (active?.get() === this) active = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = MessageSource.of(sbn.packageName) ?: return
        if (!source.enabledIn(SettingsStore.get(this).current)) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val message = NotificationText.extract(notification) ?: return
        val alert = AutoCheck.evaluate(message.text) ?: return
        val before = alerted[sbn.key]
        if (before != null && before.ordinal >= alert.level.ordinal) return
        if (alerted.size > 200) alerted.remove(alerted.keys.first())
        alerted[sbn.key] = alert.level

        when (alert.level) {
            Level.DANGER -> Alerts.showMessageDanger(this, source, message, alert.reason, sbn.key)
            Level.CAUTION -> Alerts.showMessageCaution(this, source, message, alert.reason)
            Level.SAFE -> Unit
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        alerted.remove(sbn.key)
    }

    companion object {
        @Volatile
        private var active: WeakReference<ProtectionListenerService>? = null

        /** "이 문자 지우기": 문자를 직접 지울 수는 없어서 원래 알림만 닫는다. */
        fun dismiss(notificationKey: String) {
            runCatching { active?.get()?.cancelNotification(notificationKey) }
        }
    }
}
