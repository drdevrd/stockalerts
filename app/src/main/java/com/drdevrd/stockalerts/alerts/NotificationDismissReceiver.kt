package com.drdevrd.stockalerts.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdevrd.stockalerts.data.AlertState

class NotificationDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("notif_id", -1)
        if (notifId == -1) return

        AlertState.clear(context, notifId)
        AlarmScheduler.cancelRepeat(context, notifId)
        NotificationHelper.cancel(context, notifId)
    }
}
