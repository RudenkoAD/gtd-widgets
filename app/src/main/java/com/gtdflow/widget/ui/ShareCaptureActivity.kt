package com.gtdflow.widget.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.share.ShareExtract

/**
 * Приём шаринга (ACTION_SEND text/plain, метка «GTD: во входящие»). Переиспользует тот
 * же оверлей захвата [CaptureSheet], но:
 *  • préfill — строкой из экстр шаринга (тема + текст/URL, см. [ShareExtract]);
 *  • пространство — всегда «Общее»;
 *  • после отправки закрывается (closeAfterSend), в отличие от серийного захвата.
 *
 * Если интент не тот (нет текста) — тихо закрываемся.
 */
class ShareCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefill = extractShared(intent)
        if (prefill.isBlank()) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                CaptureSheet(
                    namespace = InboxWidgetState.DEFAULT_NAMESPACE,
                    initialText = prefill,
                    closeAfterSend = true,
                    onClose = { finish() },
                )
            }
        }
    }

    private fun extractShared(intent: Intent?): String {
        if (intent == null || intent.action != Intent.ACTION_SEND) return ""
        // Экстры шаринга бывают CharSequence (Spannable), а не String — getStringExtra
        // тогда вернёт null. Берём как CharSequence и приводим к строке.
        val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        return ShareExtract.buildLine(subject, text)
    }
}
