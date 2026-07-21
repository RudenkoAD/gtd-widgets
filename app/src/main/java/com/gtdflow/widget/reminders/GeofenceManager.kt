package com.gtdflow.widget.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Напоминания по месту (геофенсы, opt-in). WHOLESALE-перевооружение после пересчёта:
 * снимаем все прежние геофенсы, ставим новые по элементам СЕГОДНЯ с местом.
 *
 * Координаты: сперва парсим строку «lat, lng» напрямую ([LocationParse]); если это имя
 * места — резолвим Geocoder.getFromLocationName (не разрешилось — молча пропускаем,
 * Log.d). Радиус 150 м, триггер ENTER. Лимит платформы — 100, наш — [MAX_GEOFENCES]=20;
 * приоритет ближайшим по времени (сортировка по началу). Без разрешений (fine + фон на
 * Q+) не ставим ничего — тумблер в настройках держится выключенным.
 */
object GeofenceManager {

    private const val TAG = "GtdRem"
    private const val RADIUS_M = 150f
    private const val MAX_GEOFENCES = 20

    /** Разделитель полей в requestId геофенса (file/line/title/place). U+001F не
     *  встречается в тексте задач/путях — безопасен для split в ресивере. */
    const val SEP = ""

    /** Перевооружить геофенсы по кандидатам (берутся только сегодняшние с местом). */
    suspend fun rearm(context: Context, candidates: List<ReminderCandidate>, todayIso: String) {
        val client = LocationServices.getGeofencingClient(context)
        val pending = geofencePendingIntent(context)

        if (!hasPermissions(context)) {
            client.removeGeofences(pending)
            Log.d(TAG, "geofence rearm skipped: no location permission")
            return
        }

        val today = candidates
            .filter { it.date == todayIso && !it.location.isNullOrBlank() }
            .sortedBy { it.startMinutes }

        val fences = ArrayList<Geofence>()
        for (c in today) {
            if (fences.size >= MAX_GEOFENCES) break
            val coords = resolve(context, c.location!!) ?: continue
            fences.add(
                Geofence.Builder()
                    .setRequestId(requestId(c))
                    .setCircularRegion(coords.lat, coords.lng, RADIUS_M)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build(),
            )
        }

        // Wholesale: снять всё прежнее, затем (если есть) поставить новое.
        client.removeGeofences(pending).addOnCompleteListener {
            if (fences.isEmpty()) return@addOnCompleteListener
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0) // не стрелять, если уже внутри при постановке
                .addGeofences(fences)
                .build()
            try {
                client.addGeofences(request, pending)
                    .addOnSuccessListener { Log.d(TAG, "geofences armed=${fences.size}") }
                    .addOnFailureListener { Log.d(TAG, "geofence add failed: ${it.message}") }
            } catch (e: SecurityException) {
                Log.d(TAG, "geofence add security: ${e.message}")
            }
        }
    }

    /** Снять все геофенсы (тумблер выключен / нет разрешений). */
    suspend fun clear(context: Context) {
        val client = LocationServices.getGeofencingClient(context)
        client.removeGeofences(geofencePendingIntent(context))
    }

    /** Есть ли разрешения для геофенса: точная геолокация + фон (на Android 10+). */
    fun hasPermissions(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    private suspend fun resolve(context: Context, location: String): LocationParse.LatLng? {
        LocationParse.parse(location)?.let { return it }
        return geocode(context, location)
    }

    private suspend fun geocode(context: Context, name: String): LocationParse.LatLng? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(name, 1)
                val addr = results?.firstOrNull() ?: run {
                    Log.d(TAG, "geocode empty for '$name'")
                    return@withContext null
                }
                LocationParse.LatLng(addr.latitude, addr.longitude)
            } catch (e: Exception) {
                Log.d(TAG, "geocode failed for '$name': ${e.message}")
                null
            }
        }

    private fun requestId(c: ReminderCandidate): String =
        listOf(c.file, c.line.toString(), c.title, c.location.orEmpty()).joinToString(SEP)

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
            .setAction(GeofenceReceiver.ACTION_GEOFENCE)
        // Геофенсинг дописывает результат в intent при срабатывании → нужен MUTABLE (S+).
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
    }
}
