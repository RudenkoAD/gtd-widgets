package com.gtdflow.widget.work

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.EngineRunner
import com.gtdflow.widget.engine.InboxSection
import com.gtdflow.widget.engine.NamespaceDef
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodaySection
import com.gtdflow.widget.engine.WidgetErrorText
import com.gtdflow.widget.engine.WidgetInput
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.inbox.InboxWidget
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.today.TodayWidget
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

/**
 * Пересчёт данных виджетов: читает vault, гоняет ядро в QuickJS, кладёт результат в
 * кэши (AppStore — «сегодня» и список пространств; Glance-состояние — «входящие»
 * каждого виджета по его пространству) и обновляет UI виджетов.
 *
 * ПОТОК ДВИЖКА: контекст QuickJS привязан к потоку. Весь блок движка выполняется в
 * withContext на ВЫДЕЛЕННОМ однопоточном диспетчере — даже сквозь suspend-точки
 * (чтение/запись Glance-состояния) корутина возвращается на тот же поток, поэтому
 * engine.compute всегда зовётся с одного потока. Диспетчер закрывается после блока.
 */
object WidgetService {

    private val todaySer = TodaySection.serializer()
    private val inboxSer = InboxSection.serializer()
    private val nsSer = ListSerializer(NamespaceDef.serializer())

    suspend fun refresh(context: Context) {
        val config = AppStore.vaultConfig(context)
        val treeUri = config.treeUri?.let(Uri::parse) ?: return
        if (!VaultManager.hasAccess(context, treeUri)) return

        val manager = GlanceAppWidgetManager(context)
        val inboxIds = manager.getGlanceIds(InboxWidget::class.java)

        try {
            // Чтение vault — на IO (много IPC к SAF-провайдеру).
            val snapshot = withContext(Dispatchers.IO) { VaultReader.read(context, treeUri) }
            val todayIso = TimeUtil.todayIso()
            val nowMinutes = TimeUtil.nowMinutes()

            EngineRunner.use(context) { engine ->
                // «Сегодня» (агрегат всех пространств) + список пространств для конфигуратора
                val base = engine.compute(
                    WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, null),
                )
                val updated = updatedFrom(base.today.generatedAt)
                AppStore.saveTodayCache(context, WidgetJson.encodeToString(todaySer, base.today), updated)
                AppStore.saveNamespacesJson(context, WidgetJson.encodeToString(nsSer, base.namespaces))

                // «Входящие» — по одному расчёту на РАЗЛИЧНОЕ пространство (мемоизация)
                val perNamespace = HashMap<String, InboxSection>()
                for (id in inboxIds) {
                    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
                    val ns = InboxWidgetState.namespaceOf(prefs)
                    val section = perNamespace.getOrPut(ns) {
                        engine.compute(
                            WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, ns),
                        ).inbox
                    }
                    updateAppWidgetState(context, id) { mutablePrefs ->
                        mutablePrefs[InboxWidgetState.INBOX_JSON] =
                            WidgetJson.encodeToString(inboxSer, section)
                        mutablePrefs[InboxWidgetState.UPDATED] = updated
                        mutablePrefs.remove(InboxWidgetState.ERROR) // успех очищает ошибку
                    }
                }
            }
        } catch (t: Throwable) {
            // ЛЮБОЙ сбой (движок/чтение vault) — не роняем воркер и не оставляем
            // виджеты в вечной «Загрузка…»: сохраняем текст ошибки, чтобы провайдеры
            // показали «Ошибка: …» (тап → приложение). Обновление UI — ниже, всегда.
            recordFailure(context, inboxIds, WidgetErrorText.forThrowable(t))
        }

        // Толкнуть перерисовку виджетов (провайдеры перечитают кэш/ошибку из состояния).
        TodayWidget().updateAll(context)
        InboxWidget().updateAll(context)
    }

    /** Записать текст ошибки в кэш «сегодня» и в состояние каждого виджета «входящих». */
    private suspend fun recordFailure(
        context: Context,
        inboxIds: List<androidx.glance.GlanceId>,
        message: String,
    ) {
        val updated = TimeUtil.minutesToHhmm(TimeUtil.nowMinutes())
        AppStore.saveTodayError(context, message, updated)
        for (id in inboxIds) {
            updateAppWidgetState(context, id) { mutablePrefs ->
                mutablePrefs[InboxWidgetState.ERROR] = message
                mutablePrefs[InboxWidgetState.UPDATED] = updated
            }
        }
    }

    /** 'YYYY-MM-DDTHH:mm' → 'HH:mm' (метка «обновлено»). */
    private fun updatedFrom(generatedAt: String): String =
        generatedAt.substringAfter('T', TimeUtil.minutesToHhmm(TimeUtil.nowMinutes()))
}
