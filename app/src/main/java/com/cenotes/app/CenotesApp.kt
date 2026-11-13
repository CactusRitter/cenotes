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
    val database: NotesDatabase by lazy {
        Room.databaseBuilder(this, NotesDatabase::class.java, "notes.db")
            .addMigrations(NotesDatabase.Migration1To2, NotesDatabase.Migration2To3)
            .build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val images: ImageStore by lazy { ImageStore(this) }

    val backup: Backup by lazy { Backup(this, database.noteDao(), images) }

    /** New versions published on GitHub (Settings → Updates and the notice at the top of the notes). */
    val updates: com.cenotes.app.update.Updates by lazy { com.cenotes.app.update.Updates(this) }

    /** Lives as long as the process: saves started while leaving a screen always finish. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
