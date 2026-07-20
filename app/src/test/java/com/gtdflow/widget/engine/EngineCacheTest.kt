package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Кэш экземпляра движка: ленивое создание, переиспользование, адресная
 * инвалидация по ошибке. Сам QuickJS требует устройства — здесь проверяется
 * именно логика кэша ([EngineCache]), от которой зависит EngineRunner.
 */
class EngineCacheTest {

    private class Res(val id: Int) {
        var closed = false
    }

    private fun cache(counter: AtomicInteger = AtomicInteger()): EngineCache<Res> =
        EngineCache(
            factory = { Res(counter.incrementAndGet()) },
            onEvict = { it.closed = true },
        )

    @Test
    fun `создание ленивое и экземпляр переиспользуется`() {
        val created = AtomicInteger()
        val c = cache(created)
        assertEquals(0, created.get())
        val a = c.acquire()
        val b = c.acquire()
        assertSame(a, b)
        assertEquals(1, created.get())
    }

    @Test
    fun `инвалидация закрывает экземпляр и следующий acquire создаёт новый`() {
        val c = cache()
        val a = c.acquire()
        assertTrue(c.invalidate(a))
        assertTrue(a.closed)
        val b = c.acquire()
        assertNotSame(a, b)
        assertFalse(b.closed)
    }

    @Test
    fun `повторная инвалидация того же экземпляра — no-op`() {
        val c = cache()
        val a = c.acquire()
        assertTrue(c.invalidate(a))
        assertFalse(c.invalidate(a))
    }

    @Test
    fun `инвалидация устаревшего экземпляра не убивает свежий`() {
        val c = cache()
        val old = c.acquire()
        c.invalidate(old)
        val fresh = c.acquire()
        // Второй упавший вызов пытается выбросить уже заменённый экземпляр
        assertFalse(c.invalidate(old))
        assertFalse(fresh.closed)
        assertSame(fresh, c.acquire())
    }

    @Test
    fun `сбой onEvict не мешает пересозданию`() {
        val c = EngineCache<Res>(
            factory = { Res(0) },
            onEvict = { throw IllegalStateException("boom") },
        )
        val a = c.acquire()
        assertTrue(c.invalidate(a))
        assertNotSame(a, c.acquire())
    }

    @Test
    fun `конкурентные acquire не создают дублей`() {
        val created = AtomicInteger()
        val c = cache(created)
        val threads = 8
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        val seen = java.util.concurrent.ConcurrentHashMap.newKeySet<Res>()
        repeat(threads) {
            pool.execute {
                start.await()
                seen.add(c.acquire())
                done.countDown()
            }
        }
        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(1, seen.size)
        assertEquals(1, created.get())
    }
}
