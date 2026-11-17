package com.example.velora

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PeriodReminderWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "period_alerts"
    }

    override fun doWork(): Result {
        val dataManager = DataManager(applicationContext)
        
        // Check if user has enabled notifications in app settings
        if (!dataManager.isNotificationsEnabled) {
            return Result.success()
        }

        val nextPeriodStr = dataManager.nextPeriodDate

        if (nextPeriodStr != null) {
            try {
                val nextPeriod = LocalDate.parse(nextPeriodStr, DateTimeFormatter.ISO_LOCAL_DATE)
                val today = LocalDate.now()
                val todayStr = today.toString()

                // 1. Avoid duplicate notifications on the same day
                if (dataManager.lastNotificationDate == todayStr) {
                    return Result.success()
                }

                val daysUntil = ChronoUnit.DAYS.between(today, nextPeriod)

                // 2. Robust check: Notify if we are in the alert window and haven't notified today
                when (daysUntil) {
                    3L -> {
                        sendNotification("Period Reminder", "Your period is expected to start in 3 days.")
                        dataManager.saveLastNotificationDate(todayStr)
                    }
                    2L -> {
                        sendNotification("Period Reminder", "Your period is expected to start in 2 days.")
                        dataManager.saveLastNotificationDate(todayStr)
                    }
                    1L -> {
                        sendNotification("Period Reminder", "Your period is expected to start tomorrow.")
                        dataManager.saveLastNotificationDate(todayStr)
                    }
                    0L -> {
                        sendNotification("Period Reminder", "Your period is expected to start today!")
                        dataManager.saveLastNotificationDate(todayStr)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Period Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Add PendingIntent to open the app when the notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT 
            else 
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
