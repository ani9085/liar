package kr.jinsil.panbyeolgi

import android.app.Application
import kr.jinsil.panbyeolgi.alert.Alerts

class JinsilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Alerts.createChannels(this)
    }
}
