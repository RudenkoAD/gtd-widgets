package com.gtdflow.widget.reminders

/**
 * Стабильный неотрицательный requestCode PendingIntent из ключа напоминания
 * (ЧИСТО, тестируется на JVM).
 *
 * Нужен детерминизм МЕЖДУ запусками процесса: перепланирование wholesale отменяет
 * «ушедшие» напоминания по сохранённым в DataStore кодам, а BOOT_COMPLETED
 * перевзводит их с нуля — код одного и того же элемента обязан совпасть. Поэтому
 * НЕ используем String.hashCode косвенно, а берём фиксированный FNV-1a (32 бита) и
 * гасим знаковый бит (requestCode должен быть неотрицательным и одинаковым всегда).
 */
object ReminderRequestCode {

    private const val FNV_OFFSET = -0x7ee3623b // 0x811c9dc5
    private const val FNV_PRIME = 0x01000193

    fun of(key: String): Int {
        var hash = FNV_OFFSET
        for (ch in key) {
            hash = hash xor ch.code
            hash *= FNV_PRIME
        }
        return hash and 0x7fffffff
    }
}
