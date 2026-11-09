package com.example.notas

import android.app.Application
import androidx.room.Room
import com.example.notas.data.ImageStore
import com.example.notas.data.NotesDatabase
import com.example.notas.data.SettingsRepository

class NotesApp : Application() {
    val database: NotesDatabase by lazy {
        Room.databaseBuilder(this, NotesDatabase::class.java, "notes.db").build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val images: ImageStore by lazy { ImageStore(this) }
}
