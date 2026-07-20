package com.gtdflow.widget.engine

/**
 * Разбор строкового ответа моста widget-bridge.js на __gtdReadCompute:
 *   'OK:'  + JSON-полезная нагрузка  → [EngineReply.Ok]
 *   'ERR:' + сообщение об ошибке      → [EngineReply.Err]
 *   всё прочее                        → [EngineReply.Err] с диагностикой.
 *
 * Вынесено из [QuickJsEngine] чистой функцией, чтобы граница движка (единственное
 * место, где Kotlin доверяет формату ответа JS) была покрыта JVM-тестом без QuickJS.
 */
sealed interface EngineReply {
    data class Ok(val payload: String) : EngineReply
    data class Err(val message: String) : EngineReply

    companion object {
        private const val OK_PREFIX = "OK:"
        private const val ERR_PREFIX = "ERR:"

        fun parse(raw: String?): EngineReply = when {
            raw == null -> Err("engine returned null reply")
            raw.startsWith(OK_PREFIX) -> Ok(raw.substring(OK_PREFIX.length))
            raw.startsWith(ERR_PREFIX) -> Err(raw.substring(ERR_PREFIX.length))
            else -> Err("unexpected engine reply: ${raw.take(64)}")
        }
    }
}
