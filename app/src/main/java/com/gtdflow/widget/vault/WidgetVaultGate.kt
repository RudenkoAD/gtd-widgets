package com.gtdflow.widget.vault

/**
 * Стартовое состояние виджета по доступу к vault (чистое решение, тестируется без
 * Context; общее для «Сегодня» и «Входящие»).
 *
 * Раньше провайдеры смотрели только на `isConfigured` и не отличали ПОТЕРЮ ДОСТУПА
 * (persistable-грант SAF отозван/папка размонтирована) от загрузки: WidgetService.refresh
 * при отсутствии доступа делает ранний return ДО записи кэша, поэтому кэш оставался
 * пустым и виджет вечно висел на «Загрузка…». Теперь потеря доступа — отдельное
 * состояние с подсказкой открыть приложение и выбрать папку заново.
 */
enum class WidgetVaultGate {
    /** Vault ещё не выбран. */
    SELECT_VAULT,

    /** Vault выбран, но доступ к папке потерян (грант отозван/папка недоступна). */
    ACCESS_LOST,

    /** Доступ есть — показываем данные/«Загрузка…»/«пусто». */
    READY,
    ;

    companion object {
        fun of(configured: Boolean, hasAccess: Boolean): WidgetVaultGate = when {
            !configured -> SELECT_VAULT
            !hasAccess -> ACCESS_LOST
            else -> READY
        }
    }
}
