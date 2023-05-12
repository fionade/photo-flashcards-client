package de.lmu.personalflashcards

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class ReminderService : BroadcastReceiver() {

    companion object {
        const val TAG = "ReminderService"
        const val REMINDER_INTENT_ID = 43
    }

    private var alarmManager: AlarmManager? = null

    fun setAlarm(context: Context?) {
        if (alarmManager != null) {
            return
        }
        // set a daily reminder notification
        alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = Intent(context, NotificationService::class.java).let { intent ->
            PendingIntent.getBroadcast(context, REMINDER_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }

        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d(TAG, "set alarm for reminder")
            setAlarm(context)
        }
    }
}