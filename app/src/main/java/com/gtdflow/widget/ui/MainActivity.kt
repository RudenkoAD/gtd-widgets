package com.gtdflow.widget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtdflow.widget.data.AppStore
import com.gtdflow.widget.engine.WidgetErrorText
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.work.DebugPreview
import com.gtdflow.widget.work.RefreshScheduler
import kotlinx.coroutines.launch

/**
 * Экран управления приложением: статус vault, выбор папки vault через SAF
 * (ACTION_OPEN_DOCUMENT_TREE + persistable-разрешение), ручное обновление виджетов,
 * текстовое превью данных (дамп «сегодня»/«входящие» для отладки) и краткая
 * инструкция по добавлению виджетов на домашний экран.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vault by AppStore.vaultConfigFlow(context)
        .collectAsState(initial = AppStore.VaultConfig(null, null))

    var previewText by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }

    val pickVault = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                // Отозванный грант / сбой SAF-провайдера не должен ронять процесс:
                // ловим всё и показываем текст ошибки в блоке превью.
                try {
                    VaultManager.persist(context, uri)
                    RefreshScheduler.refreshNow(context)
                } catch (t: Throwable) {
                    previewText = WidgetErrorText.widgetLine(t)
                }
            }
        }
    }

    fun refreshPreview() {
        previewLoading = true
        scope.launch {
            // Сбой сборки превью (движок/SAF) — показываем «Ошибка: …», не роняем процесс.
            previewText = try {
                when (val r = DebugPreview.build(context)) {
                    is DebugPreview.Result.Ready -> r.text
                    is DebugPreview.Result.Unavailable -> r.reason
                }
            } catch (t: Throwable) {
                WidgetErrorText.widgetLine(t)
            }
            previewLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(text = "GTD Flow · виджеты", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // --- Статус vault + выбор папки ---
        Text(
            text = if (vault.isConfigured) "Vault: ${vault.vaultName}" else "Vault не выбран",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pickVault.launch(null) }) {
                Text(if (vault.isConfigured) "Сменить vault" else "Выбрать vault")
            }
            OutlinedButton(onClick = { RefreshScheduler.refreshNow(context) }) {
                Text("Обновить виджеты")
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Превью данных (дамп для отладки) ---
        Text(text = "Превью данных", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { refreshPreview() }, enabled = !previewLoading) {
                Text("Собрать превью")
            }
            if (previewLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp).padding(start = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val dump = previewText
        if (dump != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = dump,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else {
            Text(
                text = "Нажмите «Собрать превью», чтобы увидеть, что виджеты покажут из выбранного vault.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Краткая инструкция ---
        Text(text = "Как пользоваться", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = INSTRUCTIONS,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val INSTRUCTIONS =
    "1. Выберите папку vault Obsidian (общая папка телефона, куда синкает Remotely Save).\n" +
        "2. Добавьте виджеты на домашний экран: «GTD · Сегодня», «GTD · Агенда» и «GTD · Входящие».\n" +
        "3. Для «Входящих» при добавлении выберите пространство (или «Общее»/«Все»), для «Агенды» — число дней (7/14/30).\n" +
        "4. Тап по заголовку виджета — обновить; тап по элементу «Сегодня»/«Агенды» — шторка деталей (правка, кнопка «В Obsidian»); тап по задаче «Входящих» — оверлей правки, чекбокс — выполнить, «＋» — быстрый захват.\n" +
        "5. Свежесть данных зависит от синка: держите Obsidian с Remotely Save в фоне.\n" +
        "Виджеты обновляются автоматически раз в ~30 минут и при каждом действии."
