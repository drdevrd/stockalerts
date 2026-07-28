package com.drdevrd.stockalerts.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Prefs {

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_FINNHUB_API_KEY = "finnhub_api_key"
    private const val KEY_GSHEETS_CSV_URL = "gsheets_csv_url"
    private const val KEY_REPEAT_INTERVAL_MIN = "repeat_interval_min"
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getFinnhubApiKey(context: Context): String =
        prefs(context).getString(KEY_FINNHUB_API_KEY, "") ?: ""

    fun setFinnhubApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_FINNHUB_API_KEY, key).apply()
    }

    fun getGoogleSheetsCsvUrl(context: Context): String =
        prefs(context).getString(KEY_GSHEETS_CSV_URL, "") ?: ""

    fun setGoogleSheetsCsvUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_GSHEETS_CSV_URL, url).apply()
    }

    fun getRepeatIntervalMinutes(context: Context): Int =
        prefs(context).getInt(KEY_REPEAT_INTERVAL_MIN, 15)

    fun setRepeatIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_REPEAT_INTERVAL_MIN, minutes).apply()
    }

    fun isFirstLaunchDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun setFirstLaunchDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }
}
