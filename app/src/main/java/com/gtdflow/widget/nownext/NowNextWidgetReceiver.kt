package com.gtdflow.widget.nownext

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.gtdflow.widget.work.RefreshScheduler

/** BroadcastReceiver виджета «Сейчас → Далее». Как «Сегодня» — держит периодику пересчёта. */
class NowNextWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowNextWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshScheduler.ensurePeriodic(context)
        // Первый «Сейчас» добавлен: одноразовый (coalesced) пересчёт — он поставит boundary-
        // аларм даже на ТЁПЛЫЙ кэш «Сегодня», где provideGlance его не ставит (гейт по
        // пустому кэшу). Иначе первый переход до ~30 мин повис бы без точного будильника.
        RefreshScheduler.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Последний «Сейчас» снят — снимаем его boundary-аларм (иначе доживёт до T, разбудит
        // устройство и погонит холостой полный пересчёт), а периодику гасим, только если не
        // осталось других наших виджетов.
        NowNextAlarms.cancel(context)
        RefreshScheduler.cancelPeriodicIfNoWidgets(context)
    }
}
