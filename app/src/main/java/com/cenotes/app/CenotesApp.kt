package com.cenotes.app

import android.app.Application
import androidx.room.Room
import com.cenotes.app.data.ImageStore
import com.cenotes.app.data.NotesDatabase
import com.cenotes.app.data.SettingsRepository

class CenotesApp : Application() {
    val database: NotesDatabase by lazy {
        Room.databaseBuilder(this, NotesDatabase::class.java, "notes.db").build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val images: ImageStore by lazy { ImageStore(this) }
}
