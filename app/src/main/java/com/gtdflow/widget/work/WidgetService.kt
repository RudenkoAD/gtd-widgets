package com.gtdflow.widget.work

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.gtdflow.widget.agenda.AgendaWidget
import com.gtdflow.widget.agenda.AgendaWidgetState
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.AgendaSection
import com.gtdflow.widget.engine.EngineRunner
import com.gtdflow.widget.engine.InboxSection
import com.gtdflow.widget.engine.NamespaceDef
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.engine.TodaySection
import com.gtdflow.widget.engine.WidgetErrorText
import com.gtdflow.widget.engine.WidgetInput
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.inbox.InboxWidget
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.nownext.NowNextAlarms
import com.gtdflow.widget.nownext.NowNextWidget
import com.gtdflow.widget.perf.Perf
import com.gtdflow.widget.reminders.ReminderCandidate
import com.gtdflow.widget.reminders.ReminderCandidates
import com.gtdflow.widget.reminders.ReminderScheduler
import com.gtdflow.widget.reminders.ReminderStore
import com.gtdflow.widget.today.TodayWidget
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultReader
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.util.concurrent.atomic.AtomicBoolean

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
    private val agendaSer = AgendaSection.serializer()
    private val nsSer = ListSerializer(NamespaceDef.serializer())

    /** Горизонт напоминаний ~36 ч укладывается в 3 дня агенды (сегодня + 2). */
    private const val REMINDER_DAYS = 3

    // --- слияние (coalesce) пересчётов ---
    private val refreshMutex = Mutex()
    private val dirty = AtomicBoolean(false)

    /**
     * Пересчёт со слиянием: если пересчёт уже идёт, повторный вызов не запускает второй,
     * а лишь взводит флаг [dirty] — активный проход, закончив, прогонит ещё раз. Так
     * двойной тап по чекбоксу (или тап + периодика) не дублируют тяжёлую работу.
     *
     * Флаг ставится ДО попытки захвата замка, поэтому активный проход гарантированно
     * увидит запрос в своём цикле `while (dirty)`. Остаётся микроскопическое окно между
     * последним `dirty=false` и `unlock()`, где запрос мог бы потеряться; для виджета это
     * безобидно — UI уже пропатчен оптимистично, а periodic/следующая интеракция сверят.
     */
    suspend fun refreshCoalesced(context: Context) {
        dirty.set(true)
        if (!refreshMutex.tryLock()) {
            Perf.mark("refresh.coalesced.skip")
            return
        }
        try {
            while (dirty.getAndSet(false)) {
                refresh(context)
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    suspend fun refresh(context: Context) {
        val config = AppStore.vaultConfig(context)
        val treeUri = config.treeUri?.let(Uri::parse) ?: return
        if (!VaultManager.hasAccess(context, treeUri)) return

        val manager = GlanceAppWidgetManager(context)
        val inboxIds = manager.getGlanceIds(InboxWidget::class.java)
        val agendaIds = manager.getGlanceIds(AgendaWidget::class.java)
        val nowNextIds = manager.getGlanceIds(NowNextWidget::class.java)

        // Снимок времени телефона — общий для расчёта и планирования напоминаний.
        val todayIso = TimeUtil.todayIso()
        val nowMinutes = TimeUtil.nowMinutes()
        // Элементы дня для аларма границы «Сейчас → Далее» (те же, что у «Сегодня»);
        // null → расчёт не удался, аларм НЕ трогаем (не снимаем при сбое чтения).
        var todayItems: List<TodayItem>? = null
        // Считать ли кандидатов на напоминания в этом проходе (гейт по настройкам —
        // не гонять лишний расчёт агенды, если оба тумблера выключены).
        val remindersActive = ReminderStore.prefs(context).anyEnabled
        // null → расчёт не удался, напоминания НЕ трогаем (не стираем при сбое чтения).
        var reminderCandidates: List<ReminderCandidate>? = null

        Perf.mark("refresh.start")
        val t0 = Perf.nowMs()
        try {
            // Чтение vault — на IO (много IPC к SAF-провайдеру).
            val snapshot = Perf.span("scan.total") {
                withContext(Dispatchers.IO) { VaultReader.read(context, treeUri) }
            }

            Perf.span("engine.total", extra = " files=${snapshot.files.size}") {
            EngineRunner.use(context) { engine ->
                // «Сегодня» (агрегат всех пространств) + список пространств для конфигуратора
                val base = Perf.span("engine.compute", extra = " kind=today") {
                    engine.compute(
                        WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, null),
                    )
                }
                val updated = updatedFrom(base.today.generatedAt)
                todayItems = base.today.items // для аларма границы «Сейчас → Далее»
                AppStore.saveTodayCache(context, WidgetJson.encodeToString(todaySer, base.today), updated)
                AppStore.saveNamespacesJson(context, WidgetJson.encodeToString(nsSer, base.namespaces))

                // «Входящие» — по одному расчёту на РАЗЛИЧНОЕ пространство (мемоизация)
                val perNamespace = HashMap<String, InboxSection>()
                for (id in inboxIds) {
                    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
                    val ns = InboxWidgetState.namespaceOf(prefs)
                    val section = perNamespace.getOrPut(ns) {
                        Perf.span("engine.compute", extra = " kind=inbox ns=$ns") {
                            engine.compute(
                                WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, ns),
                            ).inbox
                        }
                    }
                    updateAppWidgetState(context, id) { mutablePrefs ->
                        mutablePrefs[InboxWidgetState.INBOX_JSON] =
                            WidgetJson.encodeToString(inboxSer, section)
                        mutablePrefs[InboxWidgetState.UPDATED] = updated
                        mutablePrefs.remove(InboxWidgetState.ERROR) // успех очищает ошибку
                        mutablePrefs.remove(InboxWidgetState.NOTICE) // …и заметку чекбокса
                    }
                }

                // «Агенда» — по одному расчёту на РАЗЛИЧНОЕ число дней (мемоизация)
                val perDays = HashMap<Int, AgendaSection>()
                for (id in agendaIds) {
                    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
                    val days = AgendaWidgetState.daysOf(prefs)
                    val section = perDays.getOrPut(days) {
                        Perf.span("engine.compute", extra = " kind=agenda days=$days") {
                            engine.compute(
                                WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, null, days),
                            ).agenda
                        }
                    }
                    updateAppWidgetState(context, id) { mutablePrefs ->
                        mutablePrefs[AgendaWidgetState.AGENDA_JSON] =
                            WidgetJson.encodeToString(agendaSer, section)
                        mutablePrefs[AgendaWidgetState.UPDATED] = updated
                        mutablePrefs.remove(AgendaWidgetState.ERROR)
                    }
                }

                // Кандидаты на напоминания — из агенды на REMINDER_DAYS дней (сегодня + 2,
                // покрывает горизонт ~36 ч). Мемоизируем в perDays: если агенда-виджет уже
                // считает столько же дней, расчёт разделяется. Если оба тумблера выключены —
                // не считаем и НЕ трогаем планировщик (candidates остаётся null); отмену при
                // выключении тумблера делает сам экран настроек.
                if (remindersActive) {
                    val reminderAgenda = perDays.getOrPut(REMINDER_DAYS) {
                        Perf.span("engine.compute", extra = " kind=reminders days=$REMINDER_DAYS") {
                            engine.compute(
                                WidgetInput(snapshot.files, snapshot.dataJson, todayIso, nowMinutes, null, REMINDER_DAYS),
                            ).agenda
                        }
                    }
                    reminderCandidates = ReminderCandidates.from(reminderAgenda)
                }
            }
            } // engine.total
        } catch (t: Throwable) {
            // ЛЮБОЙ сбой (движок/чтение vault) — не роняем воркер и не оставляем
            // виджеты в вечной «Загрузка…»: сохраняем текст ошибки, чтобы провайдеры
            // показали «Ошибка: …» (тап → приложение). Обновление UI — ниже, всегда.
            recordFailure(context, inboxIds, agendaIds, WidgetErrorText.forThrowable(t))
        }

        // Перепланировать напоминания ТОЛЬКО при успешном расчёте (candidates != null):
        // при сбое чтения vault не стираем уже поставленные будильники/геофенсы. Ошибка
        // планирования изолирована — не роняет пересчёт и не помечает виджеты ошибкой.
        reminderCandidates?.let { candidates ->
            try {
                ReminderScheduler.onRefresh(context, candidates, todayIso)
            } catch (t: Throwable) {
                Log.d("GtdRem", "reminder scheduling failed: ${t.message}")
            }
        }

        // Перепланировать аларм границы «Сейчас → Далее» ТОЛЬКО при успешном расчёте
        // (todayItems != null): при сбое чтения vault не снимаем уже поставленный переход.
        // Нет виджета «Сейчас» (nowNextIds пуст) → аларм снимается. Ошибка планирования
        // изолирована — не роняет пересчёт. Это НЕ уведомление: разрешения не нужны.
        todayItems?.let { items ->
            try {
                NowNextAlarms.reschedule(context, nowNextIds.isNotEmpty(), items, nowMinutes, todayIso)
            } catch (t: Throwable) {
                Log.d("GtdWidget", "nownext alarm scheduling failed: ${t.message}")
            }
        }

        // Толкнуть перерисовку виджетов (провайдеры перечитают кэш/ошибку из состояния).
        Perf.span("refresh.update") {
            TodayWidget().updateAll(context)
            InboxWidget().updateAll(context)
            AgendaWidget().updateAll(context)
            NowNextWidget().updateAll(context)
        }
        Perf.mark("refresh.done total=${Perf.nowMs() - t0}ms")
    }

    /**
     * Записать текст ошибки в кэш «сегодня» и в состояние виджетов «входящих»/«агенды».
     *
     * Метку UPDATED НЕ трогаем: она — время последнего УСПЕШНОГО расчёта. Раньше сбой
     * тоже двигал её, и «обновлено HH:mm» врало о свежести кэша при устаревших данных.
     * Индикацию несвежести у кэша рисуют провайдеры (WidgetErrorText.updatedLabel).
     */
    private suspend fun recordFailure(
        context: Context,
        inboxIds: List<androidx.glance.GlanceId>,
        agendaIds: List<androidx.glance.GlanceId>,
        message: String,
    ) {
        AppStore.saveTodayError(context, message)
        for (id in inboxIds) {
            updateAppWidgetState(context, id) { mutablePrefs ->
                mutablePrefs[InboxWidgetState.ERROR] = message
            }
        }
        for (id in agendaIds) {
            updateAppWidgetState(context, id) { mutablePrefs ->
                mutablePrefs[AgendaWidgetState.ERROR] = message
            }
        }
    }

    /** 'YYYY-MM-DDTHH:mm' → 'HH:mm' (метка «обновлено»). */
    private fun updatedFrom(generatedAt: String): String =
        generatedAt.substringAfter('T', TimeUtil.minutesToHhmm(TimeUtil.nowMinutes()))
}
