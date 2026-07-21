package com.gtdflow.widget.reminders

/**
 * Чистые правила согласования тумблера «Напоминать по месту» с системными разрешениями.
 * Вынесено из Composable [com.gtdflow.widget.ui.RemindersSettings], чтобы контракт
 * «без разрешения тумблер держится выключенным» (README) можно было покрыть тестами
 * без Compose/Robolectric.
 */
object ReminderPolicy {

    /**
     * Каким должно стать persist-ленное состояние тумблера «по месту» ПОСЛЕ ответа
     * системы на запрос базовой (foreground) геолокации.
     *
     * Отказ → false: место требует хотя бы базового разрешения, иначе геофенсы не
     * встанут (ReminderScheduler логирует «permission missing despite toggle»), а README
     * обещает, что без разрешения тумблер остаётся ВЫКЛ. Выдача → true.
     *
     * Разрешение «в фоне» запрашивается ОТДЕЛЬНЫМ поэтапным шагом уже при включённом
     * тумблере (кнопка «Разрешить фон» видна только когда место включено и foreground
     * выдан), поэтому его отказ тумблер НЕ выключает — см. [placeEnabledAfterBackgroundResult].
     */
    fun placeEnabledAfterForegroundResult(granted: Boolean): Boolean = granted

    /**
     * Ответ на запрос фоновой геолокации тумблер «по месту» не меняет: фон — поэтапное
     * уточнение поверх уже включённого места. Отказ оставляет тумблер ВКЛ с красным
     * статусом «Нужен доступ в фоне», чтобы пользователь мог повторить запрос (иначе
     * кнопка «Разрешить фон» исчезла бы и фон стал недостижим).
     */
    fun placeEnabledAfterBackgroundResult(currentlyEnabled: Boolean): Boolean = currentlyEnabled
}
