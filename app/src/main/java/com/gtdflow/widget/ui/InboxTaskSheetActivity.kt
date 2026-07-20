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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.gtdflow.widget.engine.DeepLink
import com.gtdflow.widget.engine.FieldEdit
import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.LineEdits
import com.gtdflow.widget.work.EditService
import kotlinx.coroutines.launch

/**
 * Малый оверлей правки задачи из «Входящих»: текст, место, кнопки «Сохранить»
 * (buildEditedLine title/location), «Выполнено» (пометка `- [x]`), «Открыть в Obsidian»,
 * «Отмена». Задача не отдаёт rawLine из ядра — EditService находит строку по номеру+тексту.
 */
class InboxTaskSheetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val args = Args.fromIntent(intent)
        if (args == null) {
            finish()
            return
        }
        setContent {
            MaterialTheme {
                InboxTaskSheet(args = args, onClose = { finish() })
            }
        }
    }

    private data class Args(
        val title: String,
        val location: String?,
        val file: String,
        val line: Int,
        val vaultName: String?,
    ) {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                val file = intent.getStringExtra(EXTRA_FILE) ?: return null
                val line = intent.getIntExtra(EXTRA_LINE, -1)
                if (line <= 0) return null
                return Args(
                    title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                    location = intent.getStringExtra(EXTRA_LOCATION)?.takeIf { it.isNotBlank() },
                    file = file,
                    line = line,
                    vaultName = intent.getStringExtra(EXTRA_VAULT)?.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LOCATION = "location"
        private const val EXTRA_FILE = "file"
        private const val EXTRA_LINE = "line"
        private const val EXTRA_VAULT = "vault"

        fun intent(context: Context, item: InboxItem, vaultName: String?): Intent =
            Intent(context, InboxTaskSheetActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_TITLE, item.title)
                .putExtra(EXTRA_LOCATION, item.location.orEmpty())
                .putExtra(EXTRA_FILE, item.file)
                .putExtra(EXTRA_LINE, item.line)
                .putExtra(EXTRA_VAULT, vaultName.orEmpty())
    }

    @Composable
    private fun InboxTaskSheet(args: Args, onClose: () -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var title by remember { mutableStateOf(args.title) }
        var location by remember { mutableStateOf(args.location.orEmpty()) }
        var error by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }

        fun run(block: suspend () -> EditService.Outcome, successMsg: String) {
            busy = true
            error = null
            scope.launch {
                when (val outcome = block()) {
                    is EditService.Outcome.Success -> {
                        Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                    is EditService.Outcome.Failure -> {
                        busy = false
                        error = outcome.message
                    }
                }
            }
        }

        BottomOverlay(onDismiss = onClose) {
            Text(
                text = "Входящие",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Задача") },
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

            // Верхний ряд — второстепенные действия по краям карточки.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    enabled = !busy,
                    onClick = { run({ EditService.markInboxDone(context, args.file, args.line, args.title) }, "Выполнено") },
                ) { Text("Выполнено") }

                if (args.vaultName != null) {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLink.open(args.vaultName, args.file)))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(i) }
                            onClose()
                        },
                    ) { Text("В Obsidian") }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Нижний ряд — основные действия справа, как в шторке события.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose, enabled = !busy) { Text("Отмена") }
                Spacer(Modifier.width(4.dp))
                Button(
                    enabled = !busy,
                    onClick = {
                        val cleanTitle = title.trim()
                        if (cleanTitle.isEmpty()) {
                            error = "Задача не может быть пустой"
                            return@Button
                        }
                        val editsJson = LineEdits.toJson(
                            title = cleanTitle,
                            location = if (location.isBlank()) FieldEdit.Clear else FieldEdit.Set(location.trim()),
                        )
                        run({ EditService.saveInboxEdit(context, args.file, args.line, args.title, editsJson) }, "Сохранено")
                    },
                ) { Text(if (busy) "Сохранение…" else "Сохранить") }
            }
        }
    }
}
