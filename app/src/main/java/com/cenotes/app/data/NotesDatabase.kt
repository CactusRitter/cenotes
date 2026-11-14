package com.cenotes.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Label::class, NoteLabel::class], version = 5, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** 0.12 → 0.13: reminders. */
        val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `reminderAt` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `reminderRepeat` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 0.11 → 0.12: Trash, note colours and the creation date (existing notes take their last edit as it). */
        val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `trashedAt` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `notes` SET `createdAt` = `updatedAt`")
            }
        }

        /** 0.8 → 0.9: label colours (0 = automatic, so existing labels look the same). */
        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `labels` ADD COLUMN `color` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 0.7 → 0.8: archive flag and labels. Existing notes are kept untouched. */
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_labels_name` ON `labels` (`name`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_labels` (`noteId` INTEGER NOT NULL, `labelId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`noteId`, `labelId`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_labels_labelId` ON `note_labels` (`labelId`)")
            }
        }
    }
}
