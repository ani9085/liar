package kr.jinsil.panbyeolgi.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** 다른 앱으로 넘기는 동작들. 해당 앱이 없으면 짧게 알려 준다. */
object Actions {

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "이 휴대폰에서 열 수 있는 앱이 없어요", Toast.LENGTH_LONG).show()
        }
    }

    /** 112·1332: 전화 앱에 번호만 채워 연다 (바로 걸지는 않음) */
    fun dial(context: Context, number: String) {
        start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    /** 공유 시트로 글 보내기 (가족에게 물어보기) */
    fun share(context: Context, text: String, title: String = "가족에게 물어보기") {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(send, title))
    }

    /** 문자 앱에 받는 사람과 내용을 채워서 연다. SEND_SMS 권한은 쓰지 않는다. */
    fun composeSms(context: Context, phone: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}")).putExtra("sms_body", body)
        start(context, intent)
    }

    /** 보호자 번호가 있으면 보호자에게 문자, 없으면 공유 시트 */
    fun tellFamily(context: Context, guardianPhone: String, body: String) {
        if (guardianPhone.isNotBlank()) composeSms(context, guardianPhone, body) else share(context, body, "가족에게 알리기")
    }

    fun openApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
