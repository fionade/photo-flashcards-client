package de.lmu.personalflashcards

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationService: BroadcastReceiver() {

    companion object {
        const val TAG = "NotificationService"
        const val CHANNEL_ID = "ReminderNotification"
        const val CHANNEL_NAME = "Flashcard reminder"
        const val REMINDER_NOTIFICATION_ID = 1
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Alarm triggered; setting notification")

        context?.let {
//            val logger = Logger(it)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(it, 0, openIntent, 0)

            val builder = NotificationCompat.Builder(it, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_question_answer_24)
                .setContentTitle(it.getString(R.string.reminder_title))
                .setContentText(it.getString(R.string.reminder_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(it)) {

                if (Build.VERSION.SDK_INT >= 26) {
                    val notificationChannel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    createNotificationChannel(notificationChannel)
                }

//                logger.addLogMessage("sent_reminder", "")
                notify(REMINDER_NOTIFICATION_ID, builder.build())
            }
        }

    }
}