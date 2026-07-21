package com.gtdflow.widget.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gtdflow.widget.reminders.GeofenceManager
import com.gtdflow.widget.reminders.ReminderAlarms
import com.gtdflow.widget.reminders.ReminderNotifier
import com.gtdflow.widget.reminders.ReminderPolicy
import com.gtdflow.widget.reminders.ReminderStore
import com.gtdflow.widget.work.RefreshScheduler
import kotlinx.coroutines.launch

/**
 * Секция «Напоминания» экрана управления: тумблеры по времени (+ упреждение LEAD) и по
 * месту, статусы разрешений с кнопками запроса и тест-кнопка. Тумблеры пишут настройку и
 * запускают немедленный пересчёт ([RefreshScheduler.refreshNow]), который перевзводит
 * будильники/геофенсы через ReminderScheduler.
 *
 * Разрешения запрашиваются ПОЭТАПНО: уведомления (Android 13+), точные будильники (через
 * системный экран), геолокация (сначала foreground, потом фон отдельной кнопкой).
 */
@Composable
internal fun RemindersSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by ReminderStore.prefsFlow(context)
        .collectAsState(initial = ReminderStore.Prefs(false, ReminderStore.DEFAULT_LEAD, false))

    // Статусы разрешений пересчитываются при возврате на экран (ON_RESUME) и после запросов.
    var permTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifGranted = remember(permTick) { notificationsGranted(context) }
    val exactGranted = remember(permTick) { exactAlarmsGranted(context) }
    val fineGranted = remember(permTick) { fineLocationGranted(context) }
    val bgGranted = remember(permTick) { backgroundLocationGranted(context) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permTick++ }
    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permTick++
        if (ReminderPolicy.placeEnabledAfterForegroundResult(granted)) {
            RefreshScheduler.refreshNow(context)
        } else {
            // отказ в базовой геолокации: откатываем оптимистично включённый тумблер
            // (README: без разрешения место держится ВЫКЛ) и снимаем всё, что успело встать
            scope.launch {
                ReminderStore.setPlaceEnabled(context, false)
                GeofenceManager.clear(context)
            }
        }
    }
    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permTick++
        if (granted) RefreshScheduler.refreshNow(context)
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Text(text = "Напоминания", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    // --- По времени ---
    ToggleRow(
        label = "Напоминать по времени",
        checked = prefs.timeEnabled,
        onCheckedChange = { on ->
            scope.launch {
                ReminderStore.setTimeEnabled(context, on)
                if (on) {
                    RefreshScheduler.refreshNow(context) // пересчёт поставит будильники
                } else {
                    ReminderAlarms.reschedule(context, emptyList()) // выкл → снять все сразу
                }
            }
            if (on) {
                requestNotifications()
                RefreshScheduler.ensurePeriodic(context) // держать горизонт свежим и без виджетов
            }
        },
    )
    if (prefs.timeEnabled) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "За сколько предупреждать (мин):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderStore.LEAD_CHOICES.forEach { lead ->
                FilterChip(
                    selected = prefs.leadMinutes == lead,
                    onClick = {
                        scope.launch {
                            ReminderStore.setLead(context, lead)
                            RefreshScheduler.refreshNow(context)
                        }
                    },
                    label = { Text(if (lead == 0) "в момент" else "$lead") },
                )
            }
        }
        if (!notifGranted) {
            PermissionRow("Уведомления запрещены", "Разрешить") { requestNotifications() }
        }
        if (!exactGranted) {
            PermissionRow(
                "Точные будильники выключены — сработают в окне ±5 мин",
                "Настроить",
            ) { openExactAlarmSettings(context) }
        }
    }

    Spacer(Modifier.height(16.dp))

    // --- По месту ---
    ToggleRow(
        label = "Напоминать по месту (геофенс)",
        checked = prefs.placeEnabled,
        onCheckedChange = { on ->
            scope.launch {
                ReminderStore.setPlaceEnabled(context, on)
                if (on) {
                    RefreshScheduler.refreshNow(context) // пересчёт перевооружит геофенсы
                } else {
                    GeofenceManager.clear(context) // выкл → снять все геофенсы сразу
                }
            }
            if (on) {
                RefreshScheduler.ensurePeriodic(context)
                if (!fineGranted) fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
    )
    if (prefs.placeEnabled) {
        if (!fineGranted) {
            PermissionRow("Нет доступа к геолокации", "Разрешить") {
                fineLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else if (!bgGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionRow("Нужен доступ «в фоне» для срабатывания", "Разрешить фон") {
                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            Text(
                text = "Геофенсы активны для сегодняшних мест (радиус 150 м).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = {
        if (!notifGranted) requestNotifications() else ReminderNotifier.showTest(context)
    }) {
        Text("Пробное уведомление")
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionRow(status: String, action: String, onClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onClick) { Text(action) }
    }
}

// --- Статусы разрешений ---

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun exactAlarmsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(AlarmManager::class.java) ?: return false
    return am.canScheduleExactAlarms()
}

private fun fineLocationGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

private fun backgroundLocationGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
