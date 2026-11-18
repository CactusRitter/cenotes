package com.cenotes.app.notify

import com.ce.kit.log.AppLog
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.cenotes.app.CenotesApp
import com.cenotes.app.data.PlaceRadius
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch


// Reminders at a place: a note can remind you on arriving at a saved place, or on leaving it. Google Play services
// watches the places on the device itself (geofences) and wakes Cenotes when one is crossed, even while it is closed;
// the note is then shown like a reminder at a time, and the place reminder is done (like Google Keep's).

object Places {
    private const val TAG = "CenotesPlaces"
    private const val PREFIX = "note-"

    /** Whether Android lets Cenotes know where the device is ([background]: also while Cenotes is closed). */
    fun allowed(context: Context, background: Boolean = false): Boolean {
        fun has(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        val foreground = has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!background) return foreground
        return foreground && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || has(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, PlaceReceiver::class.java),
        // Mutable: Google Play services adds which places were crossed to the intent.
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0),
    )

    /** Watches the places of every note that reminds at one (and nothing else). Call after any change. */
    @SuppressLint("MissingPermission") // checked by allowed()
    suspend fun sync(context: Context) {
        val app = context.applicationContext as CenotesApp
        val dao = app.database.noteDao()
        val client = LocationServices.getGeofencingClient(context)
        runCatching { client.removeGeofences(pending(context)).await() }
        val notes = dao.placeReminders()
        if (notes.isEmpty() || !allowed(context, background = true)) {
            if (notes.isNotEmpty()) Log.w(TAG, "${notes.size} place reminder(s) waiting for the location permission")
            return
        }
        val places = dao.places().associateBy { it.id }
        val fences = notes.mapNotNull { note ->
            val place = places[note.placeId] ?: return@mapNotNull null
            Geofence.Builder()
                .setRequestId(PREFIX + note.id)
                .setCircularRegion(place.latitude, place.longitude, PlaceRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(if (note.placeLeave) Geofence.GEOFENCE_TRANSITION_EXIT else Geofence.GEOFENCE_TRANSITION_ENTER)
                // A few minutes of slack let Android save battery; a reminder at a place does not need to be to the second.
                .setNotificationResponsiveness(2 * 60 * 1000)
                .build()
        }.take(100) // Android's limit per app
        if (fences.isEmpty()) return
        val request = GeofencingRequest.Builder()
            // Already there when the reminder is set: it waits until the next time one arrives (or leaves).
            .setInitialTrigger(0)
            .addGeofences(fences)
            .build()
        runCatching { client.addGeofences(request, pending(context)).await() }
            .onSuccess { com.ce.kit.log.AppLog.i(TAG, "watching ${fences.size} place(s)") }
            .onFailure { Log.w(TAG, "could not watch the places", it) }
    }

    /** A place was crossed: the notes that remind there are shown, and their place reminders are done. */
    suspend fun fire(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "geofence error ${event.errorCode}")
            return
        }
        val app = context.applicationContext as CenotesApp
        val dao = app.database.noteDao()
        val leaving = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        for (fence in event.triggeringGeofences.orEmpty()) {
            val id = fence.requestId.removePrefix(PREFIX).toLongOrNull() ?: continue
            val note = dao.get(id) ?: continue
            if (note.placeId == null || note.placeLeave != leaving || note.trashedAt != null) continue
            Reminders.show(context, note, snooze = false)
            dao.setPlace(id, null, false)
        }
        sync(context)
        com.cenotes.app.ExternalViews.refresh(app)
    }
}

class PlaceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CenotesApp
        val result = goAsync()
        app.appScope.launch {
            try {
                Places.fire(context, intent)
            } finally {
                result.finish()
            }
        }
    }
}

/** Waits for a Google Play services task without blocking (its result, or its error thrown). */
internal suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resumeWith(Result.success(it)) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
        addOnCanceledListener { cont.cancel() }
    }
