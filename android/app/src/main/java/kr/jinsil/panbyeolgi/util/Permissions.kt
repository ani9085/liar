package kr.jinsil.panbyeolgi.util

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kr.jinsil.panbyeolgi.service.ProtectionListenerService

/** 자동 보호에 필요한 허용 상태. 설정 화면에 돌아올 때마다 다시 읽는다. */
data class ProtectionAccess(
    val notificationListener: Boolean,
    val postNotifications: Boolean,
    val fullScreenAlerts: Boolean,
    val callScreeningSupported: Boolean,
    val callScreeningRole: Boolean,
    val contacts: Boolean,
    val answerCalls: Boolean,
)

object Permissions {

    fun read(context: Context): ProtectionAccess = ProtectionAccess(
        notificationListener = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
        postNotifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        fullScreenAlerts = canUseFullScreenIntent(context),
        callScreeningSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        callScreeningRole = hasCallScreeningRole(context),
        contacts = granted(context, Manifest.permission.READ_CONTACTS),
        answerCalls = granted(context, Manifest.permission.ANSWER_PHONE_CALLS),
    )

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun canPostNotifications(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(context, Manifest.permission.POST_NOTIFICATIONS)) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    private fun hasCallScreeningRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    /** 전화 확인 앱 역할 요청 화면 (안드로이드 10 이상) */
    fun callScreeningRoleRequest(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        return roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    /** 알림 접근 허용 화면. 가능하면 진실판별기 항목을 바로 연다. */
    fun notificationListenerSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, ProtectionListenerService::class.java).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    fun appNotificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun fullScreenIntentSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
        } else {
            appNotificationSettings(context)
        }

    fun appDetailsSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
