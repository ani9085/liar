package kr.jinsil.panbyeolgi.service

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import kr.jinsil.panbyeolgi.alert.Alerts
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.util.Permissions

/**
 * 모르는 전화 경고. 설정에서 켜면 "전화 확인 앱" 역할(ROLE_CALL_SCREENING)을 받아 동작한다 (안드로이드 10 이상).
 * 전화를 막지는 않고, 연락처에 없는 번호면 경고만 띄운다.
 */
class CallGuardService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 전화는 그대로 울리게 하고 경고만 따로 띄운다.
        respondToCall(callDetails, CallResponse.Builder().build())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection != Call.Details.DIRECTION_INCOMING
        ) {
            return
        }
        if (!SettingsStore.get(this).current.callWarning) return
        // 연락처를 못 읽으면 가족 전화에도 경고가 뜨므로 아무것도 하지 않는다.
        if (!Permissions.granted(this, Manifest.permission.READ_CONTACTS)) return

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (number.isNotBlank() && isInContacts(this, number)) return
        Alerts.showCallWarning(this, number, CallRisk.classify(number))
    }

    private fun isInContacts(context: Context, number: String): Boolean = runCatching {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }.getOrDefault(false)
}
