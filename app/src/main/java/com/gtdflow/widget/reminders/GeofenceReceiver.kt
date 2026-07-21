package com.gtdflow.widget.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * Срабатывание геофенса (Play Services → сюда). На вход ENTER показываем уведомление
 * «Рядом: <задача> 📍 <место>». Данные элемента закодированы в requestId геофенса
 * (file / line / title / place через [GeofenceManager.SEP]).
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.d(TAG, "geofence event error: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val triggering = event.triggeringGeofences ?: return
        val app = context.applicationContext
        for (fence in triggering) {
            val fields = fence.requestId.split(GeofenceManager.SEP)
            val title = fields.getOrNull(2).orEmpty()
            val place = fields.getOrNull(3).orEmpty()
            val notificationId = ReminderRequestCode.of("geo:" + fence.requestId)
            ReminderNotifier.notifyPlace(app, notificationId, title, place)
        }
    }

    companion object {
        const val ACTION_GEOFENCE = "com.gtdflow.widget.reminders.ACTION_GEOFENCE"
        private const val TAG = "GtdRem"
    }
}
