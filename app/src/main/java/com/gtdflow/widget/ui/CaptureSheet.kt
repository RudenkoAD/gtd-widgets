package com.gtdflow.widget.ui

import android.widget.Toast
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gtdflow.widget.work.CaptureService
import kotlinx.coroutines.launch

/**
 * Нижний оверлей быстрого захвата (Trello-паттерн) — общий для виджета «Входящие»
 * ([CaptureActivity]) и приёма шаринга ([ShareCaptureActivity]).
 *
 * @param namespace    пространство назначения (метка + аргумент captureTargetPath).
 * @param initialText  предзаполнение поля (шаринг подставляет текст/URL; захват — пусто).
 * @param closeAfterSend  true → после успешной отправки закрыть оверлей (шаринг: одна
 *   задача и выход); false → очистить поле и остаться (серийный ввод из виджета).
 * @param onClose      закрытие оверлея (finish() активити).
 */
@Composable
internal fun CaptureSheet(
    namespace: String,
    initialText: String = "",
    closeAfterSend: Boolean = false,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    // Курсор в конце предзаполненного текста (шаринг), чтобы сразу дописывать/отправлять.
    var text by remember {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var location by remember { mutableStateOf("") }
    var showLocation by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    // Автофокус + поднятая клавиатура при открытии.
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }

    fun submit() {
        val body = text.text.trim()
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
                if (closeAfterSend) {
                    onClose() // шаринг: отправили одну задачу — закрываемся
                } else {
                    text = TextFieldValue("") // серийный ввод: очищаем, оверлей не закрываем
                    focus.requestFocus()
                    keyboard?.show()
                }
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
