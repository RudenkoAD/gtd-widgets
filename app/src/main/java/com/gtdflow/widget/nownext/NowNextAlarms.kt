package com.gtdflow.widget.nownext

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gtdflow.widget.engine.TodayItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Точный аларм границы «Сейчас → Далее» — РОВНО ОДИН на ближайший переход.
 *
 * После каждого пересчёта данных (см. WidgetService) считаем ближайшую границу показа
 * ([NowNextLogic.nextBoundaryMinutes] — min из конца current и старта next) и ставим на
 * неё один будильник; по срабатыванию [NowNextAlarmReceiver] гоняет обычный пересчёт
 * виджетов (тот же путь, что периодика), который перепланирует уже следующую границу.
 * Границ нет (нет виджета, день пуст/закончился) — снимаем старый аларм.
 *
 * Это НЕ уведомление: ReminderPolicy/разрешение на нотификации не нужны. Если точные
 * будильники недоступны (canScheduleExactAlarms=false на 12+) — тихо фолбэк на неточный
 * setAndAllowWhileIdle (переход доедет в окне, плюс подстрахует периодика раз в 30 мин).
 *
 * Состояние в DataStore не храним: аларм ровно один, requestCode фиксирован, компонент-
 * приёмник уникален — (пере)ставим FLAG_UPDATE_CURRENT либо отменяем известный PendingIntent.
 * Перевзвод после ребута отдельно не нужен: BootReceiver гоняет пересчёт, а он зовёт сюда.
 */
object NowNextAlarms {

    private const val TAG = "GtdWidget"

    // Стабильный код одного аларма. Компонент-приёмник (NowNextAlarmReceiver) уникален,
    // поэтому пересечения с кодами напоминаний (другой приёмник) невозможны.
    private const val REQUEST_CODE = 0x4E4E_0001

    /**
     * (Пере)планировать ОДИН аларм на ближайшую границу либо снять его.
     *
     * @param hasWidgets есть ли на экране хоть один виджет «Сейчас» (нет → не будим устройство).
     * @param items      элементы дня (те же, что у «Сегодня»).
     * @param nowMinutes минуты от полуночи (снимок пересчёта).
     * @param todayIso   ISO-дата пересчёта — база для перевода минут границы в момент.
     */
    fun reschedule(
        context: Context,
        hasWidgets: Boolean,
        items: List<TodayItem>,
        nowMinutes: Int,
        todayIso: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val boundary = if (hasWidgets) NowNextLogic.nextBoundaryMinutes(items, nowMinutes) else null
        if (boundary == null) {
            cancel(context, am)
            return
        }
        val date = runCatching { LocalDate.parse(todayIso) }.getOrElse { LocalDate.now(zone) }
        // plusMinutes корректно перекатывает границу за полночь (элемент без конца у полуночи).
        val triggerAtMillis = date.atStartOfDay(zone)
            .plusMinutes(boundary.toLong())
            .toInstant()
            .toEpochMilli()
        schedule(context, am, triggerAtMillis)
    }

    /**
     * Снять аларм границы вне пересчёта (последний виджет «Сейчас» снят — см.
     * NowNextWidgetReceiver.onDisabled). Идемпотентно: нет AlarmManager/аларма — no-op.
     */
    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context, am)
    }

    private fun schedule(context: Context, am: AlarmManager, triggerAtMillis: Long) {
        val pi = firePendingIntent(
            context,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
        Log.d(TAG, "nownext boundary alarm at=$triggerAtMillis exact=$exact")
    }

    private fun cancel(context: Context, am: AlarmManager) {
        val pi = firePendingIntent(
            context,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun firePendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, NowNextAlarmReceiver::class.java)
            .setAction(NowNextAlarmReceiver.ACTION_FIRE)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
