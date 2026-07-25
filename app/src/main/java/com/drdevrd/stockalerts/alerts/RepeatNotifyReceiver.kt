package com.drdevrd.stockalerts.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdevrd.stockalerts.data.AlertState
import com.drdevrd.stockalerts.data.Prefs

class RepeatNotifyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("notif_id", -1)
        if (notifId == -1) return

        if (!AlertState.isActive(context, notifId)) return // user already dismissed it

        val title = AlertState.getTitle(context, notifId)
        val message = AlertState.getMessage(context, notifId)
        NotificationHelper.show(context, notifId, title, message)

        AlarmScheduler.scheduleRepeat(context, notifId, Prefs.getRepeatIntervalMinutes(context))
    }
}
