package com.gtdflow.widget.work

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Регресс-защита: интерактивные пересчёты ушли с WorkManager (где doWork глотал Throwable)
 * на корневые корутины [AppScope]. Область ОБЯЗАНА нести CoroutineExceptionHandler, иначе
 * сбой пересчёта (Glance updateAll → TransactionTooLargeException/DeadObjectException,
 * движок → Error) уходит в Thread.uncaughtExceptionHandler и роняет процесс.
 */
class AppScopeTest {

    @Test
    fun scopeCarriesExceptionHandler() {
        // Без обработчика непойманный throw корневой корутины роняет процесс.
        assertNotNull(AppScope.scope.coroutineContext[CoroutineExceptionHandler])
    }

    @Test
    fun throwingJobIsSwallowedAndScopeSurvives() = runBlocking {
        // Падение одного фонового пересчёта не должно ни ронять процесс (обработчик
        // проглотил бы его вместо Thread.uncaughtExceptionHandler), ни отменять область.
        val failed = AppScope.scope.launch { throw RuntimeException("boom") }
        failed.join()
        assertTrue("корутина должна завершиться падением", failed.isCancelled)

        // Область (SupervisorJob) жива — следующая интеракция всё ещё считается.
        val ran = AtomicBoolean(false)
        AppScope.scope.launch { ran.set(true) }.join()
        assertTrue("scope должен пережить падение предыдущей корутины", ran.get())
    }
}
