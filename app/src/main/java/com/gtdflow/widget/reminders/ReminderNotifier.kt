package com.gtdflow.widget.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gtdflow.widget.R
import com.gtdflow.widget.ui.MainActivity

/**
 * Канал и показ уведомлений напоминаний.
 *
 * Один канал «Напоминания GTD» (id [CHANNEL_ID]) для обоих видов — по времени и по
 * месту. Тап по уведомлению открывает приложение (MainActivity). На Android 13+ показ
 * молча пропускается, если не выдано POST_NOTIFICATIONS (тумблер напоминаний в
 * настройках сам просит это разрешение).
 */
object ReminderNotifier {

    const val CHANNEL_ID = "gtd_reminders"
    private const val TAG = "GtdRem"
    private const val TEST_NOTIFICATION_ID = 424242

    /** Создать канал (идемпотентно). Зовётся перед любым показом. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminders_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminders_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /** Напоминание по времени: заголовок = название, текст = «HH:mm… · 📍… · пространство». */
    fun notifyTime(context: Context, notificationId: Int, title: String, text: String) {
        show(context, notificationId, title.ifBlank { text }, text)
    }

    /** Напоминание по месту: «Рядом: <задача> 📍 <место>». */
    fun notifyPlace(context: Context, notificationId: Int, title: String, place: String) {
        val heading = "Рядом: ${title.ifBlank { "задача" }}"
        val body = if (place.isBlank()) heading else "$heading 📍 $place"
        show(context, notificationId, heading, body)
    }

    /** Пробное уведомление из настроек (проверка канала/разрешения). */
    fun showTest(context: Context) {
        show(
            context,
            TEST_NOTIFICATION_ID,
            context.getString(R.string.reminders_channel_name),
            "Пробное напоминание · сейчас",
        )
    }

    private fun show(context: Context, notificationId: Int, title: String, text: String) {
        ensureChannel(context)
        if (!canPost(context)) {
            Log.d(TAG, "notification skipped: POST_NOTIFICATIONS not granted")
            return
        }
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Гонка отзыва разрешения между проверкой и показом — не падаем.
            Log.d(TAG, "notify failed: ${e.message}")
        }
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
