package kr.jinsil.panbyeolgi.alert

import android.app.Activity
import android.os.Build
import android.view.WindowManager

/** 잠금 화면 위에도 경고를 보여 주고 화면을 켠다. */
fun Activity.showOverLockScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
}
