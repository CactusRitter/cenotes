package com.cenotes.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cenotes.app.CenotesApp
import com.cenotes.app.LaunchAction
import com.cenotes.app.MainActivity
import com.cenotes.app.R
import com.cenotes.app.data.Note
import com.cenotes.app.data.Repeat
import com.cenotes.app.ui.markdown.markdownToPlainText
import kotlinx.coroutines.launch

// Reminders (docs/ROADMAP.md, phase 2): a note can remind you at a time, once or repeating. One exact alarm is set for
// the earliest reminder; when it goes off, every reminder that is due is shown (and a repeating one moves on), and the
// alarm is set again for the next. Notifications look like Cemail's: tap opens the note, plus two quick actions.

object Reminders {
    private const val TAG = "CenotesReminders"
    private const val CHANNEL = "reminders"
    const val ACTION_FIRE = "com.cenotes.app.REMINDER_FIRE"
    const val ACTION_DONE = "com.cenotes.app.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.cenotes.app.REMINDER_SNOOZE"
    const val EXTRA_NOTE = "note_id"
    private const val SNOOZE_MS = 60L * 60 * 1000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH)
        channel.description = context.getString(R.string.reminder_channel_body)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** true if Android lets Cenotes show notifications (asked for from Android 13). */
    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Sets the alarm for the earliest reminder (or removes it when there is none). Call after any change. */
    suspend fun schedule(context: Context) {
        val app = context.applicationContext as CenotesApp
        val next = app.database.noteDao().nextReminder()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (next == null) {
            alarms.cancel(pending)
            return
        }
        val at = maxOf(next, System.currentTimeMillis() + 1000)
        // Exact when Android allows it (it does for Cenotes from Android 13, with USE_EXACT_ALARM); otherwise as close
        // as Android lets it be, even in battery saving.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
        Log.i(TAG, "next reminder in ${(at - System.currentTimeMillis()) / 1000} s")
    }

    /** Shows every reminder that is due, moves repeating ones on (the others are done) and sets the next alarm. */
    suspend fun fire(context: Context) {
        val app = context.applicationContext as CenotesApp
        val dao = app.database.noteDao()
        val due = dao.dueReminders(System.currentTimeMillis())
        for (note in due) {
            show(context, note)
            val repeat = Repeat.of(note.reminderRepeat)
            val next = note.reminderAt?.let { repeat.next(it) }
            dao.setReminder(note.id, next, if (next == null) 0 else note.reminderRepeat)
        }
        Log.i(TAG, "${due.size} reminder(s) shown")
        schedule(context)
    }

    @SuppressLint("MissingPermission") // checked by allowed()
    private fun show(context: Context, note: Note) {
        if (!allowed(context)) return
        val id = note.id.toInt()
        val text = markdownToPlainText(note.content).trim()
        val title = note.title.ifBlank { text.lineSequence().firstOrNull().orEmpty() }.ifBlank { context.getString(R.string.untitled) }
        val body = if (note.title.isBlank()) text.lineSequence().drop(1).joinToString("\n").trim() else text
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .setAction(LaunchAction.ACTION_OPEN_NOTE)
                .putExtra(LaunchAction.EXTRA_NOTE_ID, note.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.take(200))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(1000)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, context.getString(R.string.reminder_done), action(context, ACTION_DONE, note.id, 1))
        // "In an hour" only for reminders that happen once: a repeating one already comes back by itself.
        if (Repeat.of(note.reminderRepeat) == Repeat.NONE) {
            builder.addAction(0, context.getString(R.string.reminder_snooze), action(context, ACTION_SNOOZE, note.id, 2))
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun action(context: Context, action: String, noteId: Long, slot: Int): PendingIntent = PendingIntent.getBroadcast(
        context, noteId.toInt() * 10 + slot,
        Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_NOTE, noteId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** "In an hour": the notification goes and the note reminds again an hour from now. */
    suspend fun snooze(context: Context, noteId: Long) {
        val app = context.applicationContext as CenotesApp
        NotificationManagerCompat.from(context).cancel(noteId.toInt())
        app.database.noteDao().setReminder(noteId, System.currentTimeMillis() + SNOOZE_MS, 0)
        schedule(context)
    }
}

/** The alarm going off, the quick actions of a reminder, and the phone starting up (alarms do not survive it). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CenotesApp
        val result = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    Reminders.ACTION_FIRE -> Reminders.fire(context)
                    Reminders.ACTION_DONE -> NotificationManagerCompat.from(context).cancel(intent.getLongExtra(Reminders.EXTRA_NOTE, 0).toInt())
                    Reminders.ACTION_SNOOZE -> Reminders.snooze(context, intent.getLongExtra(Reminders.EXTRA_NOTE, 0))
                    // After a restart, an update of Cenotes or a change of the clock: reminders that went by meanwhile
                    // are shown now, and the alarm is set again.
                    else -> Reminders.fire(context)
                }
            } finally {
                result.finish()
            }
        }
    }
}
