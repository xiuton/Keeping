package me.ganto.keeping.feature.my

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import me.ganto.keeping.R
import android.app.AlarmManager
import android.app.PendingIntent

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 读取DataStore中的hour/minute，重新注册闹钟
            val dataStore = context.applicationContext.getSharedPreferences("datastore.preferences", Context.MODE_PRIVATE)
            val hour = dataStore.getString("reminder_time", "20:00")?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 20
            val minute = dataStore.getString("reminder_time", "20:00")?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
            val enabled = dataStore.getString("reminder_enabled", "false") == "true"
            if (enabled) {
                // 重新注册闹钟
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val nextIntent = Intent(context, ReminderReceiver::class.java).apply {
                    action = "me.ganto.keeping.ACTION_REMIND"
                    putExtra("hour", hour)
                    putExtra("minute", minute)
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, 0, nextIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
                }
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            return
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "记账提醒", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("记账提醒")
            .setContentText("该记账啦！")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1001, notification)

        // 读取上次的hour/minute
        val hour = intent.getIntExtra("hour", 20)
        val minute = intent.getIntExtra("minute", 0)
        // 触发后自动注册下一次
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "me.ganto.keeping.ACTION_REMIND"
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
} 