package com.gtdflow.widget.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import com.gtdflow.widget.inbox.InboxWidgetState

/**
 * Быстрый захват во «Входящие» (Trello-паттерн): нижний оверлей поверх домашнего
 * экрана. Всю механику несёт общий [CaptureSheet] (поле с автофокусом и поднятой
 * клавиатурой, свёрнутое поле места, серийный ввод — после отправки поле очищается,
 * оверлей остаётся). Пространство приходит экстрой от виджета «Входящие» (при «Все»
 * захват уходит в «Общее», см. CaptureNamespace). Закрыть — тап по затемнению.
 */
class CaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val namespace = intent.getStringExtra(EXTRA_NAMESPACE) ?: InboxWidgetState.DEFAULT_NAMESPACE
        setContent {
            MaterialTheme {
                CaptureSheet(namespace = namespace, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_NAMESPACE = "namespace"

        /** Интент запуска захвата в указанное пространство (используется виджетом «Входящие»). */
        fun intent(context: Context, namespace: String): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_NAMESPACE, namespace)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
