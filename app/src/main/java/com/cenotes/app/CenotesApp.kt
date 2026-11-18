package com.cenotes.app

import android.app.Application
import androidx.room.Room
import com.cenotes.app.data.Backup
import com.cenotes.app.data.ImageStore
import com.cenotes.app.data.NotesDatabase
import com.cenotes.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CenotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.ce.kit.CeKit.init(this, com.ce.kit.CeAppConfig("Cenotes", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, "CactusRitter/cenotes"))
        com.cenotes.app.notify.Reminders.createChannel(this)
    }

    val database: NotesDatabase by lazy {
        Room.databaseBuilder(this, NotesDatabase::class.java, "notes.db")
            .addMigrations(NotesDatabase.Migration1To2, NotesDatabase.Migration2To3, NotesDatabase.Migration3To4, NotesDatabase.Migration4To5, NotesDatabase.Migration5To6, NotesDatabase.Migration6To7, NotesDatabase.Migration7To8, NotesDatabase.Migration8To9)
            .build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val images: ImageStore by lazy { ImageStore(this) }

    val backup: Backup by lazy { Backup(this, database.noteDao(), images, settings) }

    /** New versions published on GitHub (Settings → Updates and the notice at the top of the notes). */
    val updates: com.ce.kit.update.Updates get() = com.ce.kit.CeKit.updates

    /** Lives as long as the process: saves started while leaving a screen always finish. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + kotlinx.coroutines.CoroutineExceptionHandler { _, error -> com.ce.kit.log.CrashLog.write(this, error) })
}
