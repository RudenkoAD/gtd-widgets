package com.gtdflow.widget.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.gtdflow.widget.agenda.AgendaDayHeader
import com.gtdflow.widget.engine.DeepLink
import com.gtdflow.widget.engine.FieldEdit
import com.gtdflow.widget.engine.LineEdits
import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem
import com.gtdflow.widget.work.EditService
import kotlinx.coroutines.launch

/**
 * Шторка деталей события/задачи (как тап по событию в Google Calendar): нижний оверлей
 * поверх домашнего экрана. Показывает название, дату+время, «Повторяется: …» для серий,
 * 📍 место; даёт править название, дату (для задач/одноразовых), время, место. Кнопки:
 * «Сохранить» (buildEditedLine + запись по точному rawLine), «Открыть в Obsidian»,
 * «Отмена». Для вхождения серии дата не редактируется — правится ВСЯ серия.
 *
 * Все данные приходят экстрами (см. [intent]); rawLine — точный якорь строки файла.
 */
class EventSheetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Рисуем под системными панелями → imePadding/navigationBarsPadding работают.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val args = SheetArgs.fromIntent(intent)
        if (args == null) {
            finish()
            return
        }
        setContent {
            MaterialTheme {
                EventSheet(args = args, onClose = { finish() })
            }
        }
    }

    /** Разобранные экстры шторки. */
    private data class SheetArgs(
        val itemKind: String,
        val title: String,
        val dayIso: String,
        val startMinutes: Int?,
        val endMinutes: Int?,
        val location: String?,
        val file: String,
        val line: Int,
        val rawLine: String,
        val recurrenceText: String?,
        val namespace: String,
        val vaultName: String?,
    ) {
        val isSeries: Boolean get() = itemKind == "series-occurrence"

        companion object {
            fun fromIntent(intent: Intent): SheetArgs? {
                val file = intent.getStringExtra(EXTRA_FILE) ?: return null
                val rawLine = intent.getStringExtra(EXTRA_RAW_LINE) ?: return null
                val line = intent.getIntExtra(EXTRA_LINE, -1)
                if (line <= 0 || rawLine.isEmpty()) return null
                return SheetArgs(
                    itemKind = intent.getStringExtra(EXTRA_ITEM_KIND).orEmpty(),
                    title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                    dayIso = intent.getStringExtra(EXTRA_DAY).orEmpty(),
                    startMinutes = intent.getIntExtra(EXTRA_START, -1).takeIf { it >= 0 },
                    endMinutes = intent.getIntExtra(EXTRA_END, -1).takeIf { it >= 0 },
                    location = intent.getStringExtra(EXTRA_LOCATION)?.takeIf { it.isNotBlank() },
                    file = file,
                    line = line,
                    rawLine = rawLine,
                    recurrenceText = intent.getStringExtra(EXTRA_RECURRENCE)?.takeIf { it.isNotBlank() },
                    namespace = intent.getStringExtra(EXTRA_NAMESPACE).orEmpty(),
                    vaultName = intent.getStringExtra(EXTRA_VAULT)?.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ITEM_KIND = "itemKind"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_DAY = "day"
        private const val EXTRA_START = "start"
        private const val EXTRA_END = "end"
        private const val EXTRA_LOCATION = "location"
        private const val EXTRA_FILE = "file"
        private const val EXTRA_LINE = "line"
        private const val EXTRA_RAW_LINE = "rawLine"
        private const val EXTRA_RECURRENCE = "recurrence"
        private const val EXTRA_NAMESPACE = "namespace"
        private const val EXTRA_VAULT = "vault"

        /** Интент шторки для элемента ленты [item], показанного на дне [dayIso]. */
        fun intent(context: Context, item: TodayItem, dayIso: String, vaultName: String?): Intent =
            Intent(context, EventSheetActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ITEM_KIND, item.itemKind.ifEmpty { if (item.isEvent) "single-event" else "task" })
                .putExtra(EXTRA_TITLE, item.title)
                .putExtra(EXTRA_DAY, dayIso)
                .putExtra(EXTRA_START, item.startMinutes ?: -1)
                .putExtra(EXTRA_END, item.endMinutes ?: -1)
                .putExtra(EXTRA_LOCATION, item.location.orEmpty())
                .putExtra(EXTRA_FILE, item.file)
                .putExtra(EXTRA_LINE, item.line)
                .putExtra(EXTRA_RAW_LINE, item.rawLine)
                .putExtra(EXTRA_RECURRENCE, item.recurrenceText.orEmpty())
                .putExtra(EXTRA_NAMESPACE, item.namespace)
                .putExtra(EXTRA_VAULT, vaultName.orEmpty())
    }

    @Composable
    private fun EventSheet(args: SheetArgs, onClose: () -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var title by remember { mutableStateOf(args.title) }
        var date by remember { mutableStateOf(args.dayIso) }
        var time by remember { mutableStateOf(TimeUtil.editableRange(args.startMinutes, args.endMinutes)) }
        var location by remember { mutableStateOf(args.location.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }
        var saving by remember { mutableStateOf(false) }

        BottomOverlay(onDismiss = onClose) {
            // Заголовок-строка: дата + время + вид.
            Text(
                text = AgendaDayHeader.format(args.dayIso) +
                    TimeUtil.formatRange(args.startMinutes, args.endMinutes).let { if (it.isEmpty()) "" else " · $it" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (args.isSeries) {
                Text(
                    text = "Повторяется" + (args.recurrenceText?.let { ": $it" } ?: "") + " · правка всей серии",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (args.namespace.isNotBlank()) {
                Text(
                    text = args.namespace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            if (!args.isSeries) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Время (ЧЧ:ММ или ЧЧ:ММ-ЧЧ:ММ)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("📍 Место") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            val err = error
            if (err != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose, enabled = !saving) { Text("Отмена") }
                if (args.vaultName != null) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            openInObsidian(context, args.vaultName, args.file)
                            onClose()
                        },
                        enabled = !saving,
                    ) { Text("В Obsidian") }
                }
                Spacer(Modifier.width(4.dp))
                Button(
                    enabled = !saving,
                    onClick = {
                        val cleanTitle = title.trim()
                        if (cleanTitle.isEmpty()) {
                            error = "Название не может быть пустым"
                            return@Button
                        }
                        saving = true
                        error = null
                        val editsJson = LineEdits.toJson(
                            title = cleanTitle,
                            date = if (args.isSeries) FieldEdit.Keep else fieldEditOf(date),
                            timeRange = fieldEditOf(time),
                            location = fieldEditOf(location),
                        )
                        scope.launch {
                            val outcome = EditService.saveTodayEdit(
                                context, args.file, args.line, args.rawLine, editsJson,
                            )
                            when (outcome) {
                                is EditService.Outcome.Success -> {
                                    Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                                    onClose()
                                }
                                is EditService.Outcome.Failure -> {
                                    saving = false
                                    error = outcome.message
                                }
                            }
                        }
                    },
                ) { Text(if (saving) "Сохранение…" else "Сохранить") }
            }
        }
    }
}

/** Пустое поле → снять (Clear); непустое → установить (Set). */
private fun fieldEditOf(value: String): FieldEdit {
    val v = value.trim()
    return if (v.isEmpty()) FieldEdit.Clear else FieldEdit.Set(v)
}

private fun openInObsidian(context: Context, vaultName: String, file: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLink.open(vaultName, file)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
