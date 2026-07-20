package com.gtdflow.widget.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.work.CaptureService
import kotlinx.coroutines.launch

/**
 * Быстрый захват во «Входящие» (Trello-паттерн): нижний оверлей поверх домашнего
 * экрана. Поле «Что во входящие?» с автофокусом и поднятой клавиатурой (adjustResize +
 * показ IME), кнопка-стрелка отправки, свёрнутое поле «📍 Место» (иконка раскрывает).
 * После отправки — тост и ОЧИСТКА поля (серийный ввод: сыпать задачи одну за другой,
 * не закрывая оверлей). Закрыть — тап по затемнению. Пространство приходит экстрой от
 * виджета «Входящие» (при «Все» захват уходит в «Общее», см. CaptureNamespace).
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

@Composable
private fun CaptureSheet(namespace: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    var text by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showLocation by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    // Автофокус + поднятая клавиатура при открытии.
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    fun submit() {
        val body = text.trim()
        if (body.isEmpty() || sending) return
        sending = true
        val loc = location.trim().ifEmpty { null }
        scope.launch {
            val outcome = CaptureService.capture(context, namespace, body, loc)
            val msg = when (outcome) {
                is CaptureService.Outcome.Success -> "Во входящие"
                is CaptureService.Outcome.Failure -> outcome.message
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            if (outcome is CaptureService.Outcome.Success) {
                text = "" // серийный ввод: очищаем поле, оверлей не закрываем
                focus.requestFocus()
                keyboard?.show()
            }
            sending = false
        }
    }

    BottomOverlay(onDismiss = onClose) {
        Text(
            text = "Входящие · $namespace",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Что во входящие?") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                trailingIcon = {
                    IconButton(onClick = { showLocation = !showLocation }) {
                        Text("📍")
                    }
                },
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { submit() }, enabled = !sending) {
                Text(
                    text = "➤",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (showLocation) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("📍 Место") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
    }
}
