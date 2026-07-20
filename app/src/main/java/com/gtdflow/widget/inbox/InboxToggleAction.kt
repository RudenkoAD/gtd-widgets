package com.gtdflow.widget.inbox

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.gtdflow.widget.perf.Perf
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultWriter
import com.gtdflow.widget.work.OptimisticInbox
import com.gtdflow.widget.work.WidgetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Отметка входящей задачи выполненной по тапу чекбокс-зоны строки.
 *
 * Привязка — ОБЫЧНЫЙ `clickable(actionRunCallback<InboxToggleAction>())` на охватывающем
 * Box с визуальным глифом чекбокса (см. InboxWidget.InboxRow), а НЕ `onCheckedChange` у
 * Glance CheckBox. Причина: compound-button после тапа сам перерисовывался из состояния ДО
 * оптимистичной записи и перекрывал наш `updateAll` — строка «висла» до полного пересчёта
 * (~1.9 c). С обычным clickable патч рисуется так же быстро, как оптимистичный захват «+».
 *
 * Порядок ради МГНОВЕННОГО отклика (раньше строка «висела» ~5 c до конца пересчёта):
 *  1. ОПТИМИСТИЧНО убрать строку из кэша всех виджетов «Входящие» и перерисовать —
 *     визуальный отклик за десятки мс, ещё ДО записи в файл.
 *  2. Записать `- [x]` в файл (перечитывает актуальный файл, ищет строку по номеру,
 *     затем по тексту — см. TaskLineToggle; промах → строка вернётся на шаге 3).
 *  3. Полный пересчёт из vault ИНЛАЙН со слиянием (goAsync держит процесс живым):
 *     сверяет все виджеты с честным состоянием. Промах записи → строка вернётся;
 *     сбой движка → виджет покажет ошибку.
 */
class InboxToggleAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Perf.mark("toggle.received")
        val file = parameters[KEY_FILE]
        val line = parameters[KEY_LINE]
        val title = parameters[KEY_TITLE]

        // 1. Оптимистичный отклик — до записи и пересчёта.
        if (file != null && line != null) {
            OptimisticInbox.removeLine(context, file, line)
        }

        // 2. Запись в файл (IO).
        if (file != null && line != null && title != null) {
            val treeUri = VaultManager.treeUri(context)
            if (treeUri != null && VaultManager.hasAccess(context, treeUri)) {
                Perf.span("toggle.write") {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            VaultWriter.markInboxDone(context, treeUri, file, line, title)
                        }
                    }
                }
            }
        }

        // 3. Честный пересчёт инлайн (со слиянием двойных тапов). Экшен ЖДЁТ пересчёт
        //    прямо под goAsync (не через AppScope), поэтому ловим ЛЮБОЙ Throwable здесь:
        //    сбой движка приходит Error'ом (UnsatisfiedLinkError), а Glance updateAll внутри
        //    refresh() идёт вне его try/catch и может кинуть TransactionTooLargeException на
        //    большом списке / DeadObjectException при рестарте system_server. Без ловли такой
        //    throw уходит в корутину goAsync и роняет процесс. UI уже пропатчен оптимистично,
        //    а WidgetService показал ошибку в виджетах — здесь остаётся лишь не рухнуть (тот
        //    же контракт, что catch(Throwable) в WidgetRefreshWorker.doWork).
        try {
            WidgetService.refreshCoalesced(context)
        } catch (t: Throwable) {
            Log.e("GtdWidget", "inbox toggle recompute crashed", t)
        }
    }

    companion object {
        val KEY_FILE = ActionParameters.Key<String>("file")
        val KEY_LINE = ActionParameters.Key<Int>("line")
        val KEY_TITLE = ActionParameters.Key<String>("title")
    }
}
