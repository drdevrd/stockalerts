package com.drdevrd.stockalerts.data

import android.content.Context

/** Tracks the currently-active repeating alert (if any) per notification id. */
object AlertState {

    private fun prefs(context: Context) =
        context.getSharedPreferences("alert_state", Context.MODE_PRIVATE)

    fun setActive(context: Context, notifId: Int, title: String, message: String) {
        prefs(context).edit()
            .putBoolean("active_$notifId", true)
            .putString("title_$notifId", title)
            .putString("message_$notifId", message)
            .apply()
    }

    fun isActive(context: Context, notifId: Int): Boolean =
        prefs(context).getBoolean("active_$notifId", false)

    fun getTitle(context: Context, notifId: Int): String =
        prefs(context).getString("title_$notifId", "") ?: ""

    fun getMessage(context: Context, notifId: Int): String =
        prefs(context).getString("message_$notifId", "") ?: ""

    fun clear(context: Context, notifId: Int) {
        prefs(context).edit()
            .putBoolean("active_$notifId", false)
            .apply()
    }
}
