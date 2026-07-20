package com.gtdflow.widget.work

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Процессная корутин-область для фоновой работы, которая должна ПЕРЕЖИТЬ породивший
 * её компонент (Glance-экшен после goAsync, оверлей-Activity после finish()).
 *
 * Интерактивные пересчёты виджетов больше не идут через WorkManager (его латентность
 * постановки — сотни мс, см. метки GtdPerf «workmanager.enqueue → worker.start»).
 * Вместо этого действие делает оптимистичный патч state (мгновенный отклик) и запускает
 * полный пересчёт здесь — он завершится в фоне, даже если Activity уже закрыта.
 * WorkManager остаётся только для ПЕРИОДИКИ (RefreshScheduler.ensurePeriodic).
 *
 * SupervisorJob — падение одного пересчёта не роняет область; Dispatchers.Default —
 * лёгкий пул, а тяжёлое IO/движок внутри пересчёта сами уходят на свои диспетчеры.
 *
 * [crashGuard] — обязателен. Пересчёт запускается как КОРНЕВАЯ корутина (launch на этой
 * области), а часть refresh() выполняется ВНЕ её внутреннего try/catch: подготовка перед
 * сканом и, главное, финальный updateAll (Glance может кинуть TransactionTooLargeException
 * на большом списке или DeadObjectException при рестарте system_server; сбой движка вообще
 * приходит Error'ом — UnsatisfiedLinkError). Без обработчика такой Throwable в корневой
 * корутине уходит в Thread.uncaughtExceptionHandler и роняет процесс. Раньше этот путь шёл
 * через WorkManager, где WidgetRefreshWorker.doWork глотал Throwable; тот же контракт нужно
 * сохранить и здесь. Сам обработчик логирует под runCatching — он НИКОГДА не должен бросать.
 */
object AppScope {
    private const val TAG = "GtdWidget"

    private val crashGuard = CoroutineExceptionHandler { _, throwable ->
        runCatching { Log.e(TAG, "background widget refresh crashed", throwable) }
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + crashGuard)
}
