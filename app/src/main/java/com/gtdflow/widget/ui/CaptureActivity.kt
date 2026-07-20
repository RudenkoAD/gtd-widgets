package com.gtdflow.widget.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gtdflow.widget.inbox.InboxWidgetState
import com.gtdflow.widget.work.CaptureService
import kotlinx.coroutines.launch

/**
 * Быстрый захват во «Входящие» (плейсхолдер зоны окружения). Диалоговая тема поверх
 * домашнего экрана: поле текста + поле места (📍), кнопка «Во входящие» вызывает
 * CaptureService (строит строку/путь ЯДРОМ и пишет в vault). Пространство приходит
 * экстрой от виджета «Входящие».
 */
class CaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val namespace = intent.getStringExtra(EXTRA_NAMESPACE) ?: InboxWidgetState.DEFAULT_NAMESPACE
        setContent {
            MaterialTheme {
                Surface {
                    CaptureScreen(namespace = namespace, onDone = { finish() })
                }
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
private fun CaptureScreen(namespace: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(20.dp)) {
        Text(text = "Входящие · $namespace", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Что во входящие?") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Место (📍, необязательно)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDone) { Text("Отмена") }
            Spacer(Modifier.height(0.dp))
            Button(
                onClick = {
                    val body = text.trim()
                    if (body.isEmpty()) {
                        onDone()
                        return@Button
                    }
                    val loc = location.trim().ifEmpty { null }
                    scope.launch {
                        val outcome = CaptureService.capture(context, namespace, body, loc)
                        val msg = when (outcome) {
                            is CaptureService.Outcome.Success -> "Во входящие"
                            is CaptureService.Outcome.Failure -> outcome.message
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                },
            ) { Text("Во входящие") }
        }
    }
}
