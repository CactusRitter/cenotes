package com.cenotes.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Label::class, NoteLabel::class, NoteVersion::class, Place::class, DeletedNote::class], version = 9, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** Every migration, oldest first: the app and the migration tests use this same list. */
        val AllMigrations: Array<Migration> by lazy {
            arrayOf(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6, Migration6To7, Migration7To8, Migration8To9)
        }

        /**
         * 1.5 → 1.6: every note gets a stable UUID, a "modified" clock (starting at its last text edit) and deleted notes
         * are remembered. Room runs this in one transaction, so if anything does not add up it throws and the database
         * stays as it was (version 8, nothing lost).
         */
        val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val before = db.query("SELECT COUNT(*) FROM `notes`").use { it.moveToFirst(); it.getLong(0) }
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `uuid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `modifiedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `notes` SET `modifiedAt` = `updatedAt`")
                val ids = db.query("SELECT `id` FROM `notes`").use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }
                val seen = HashSet<String>()
                for (id in ids) {
                    var uuid = newUuid()
                    while (!seen.add(uuid)) uuid = newUuid()
                    db.execSQL("UPDATE `notes` SET `uuid` = ? WHERE `id` = ?", arrayOf<Any>(uuid, id))
                }
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notes_uuid` ON `notes` (`uuid`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `deleted_notes` (`uuid` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`uuid`))")
                val after = db.query("SELECT COUNT(*) FROM `notes`").use { it.moveToFirst(); it.getLong(0) }
                val withUuid = db.query("SELECT COUNT(DISTINCT `uuid`) FROM `notes` WHERE `uuid` <> ''").use { it.moveToFirst(); it.getLong(0) }
                check(after == before && withUuid == before) { "Migration 8→9 does not add up: $before notes, $after after, $withUuid with uuid" }
            }
        }

        /** 0.16 → 0.17: reminders at a place. */
        val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `placeId` INTEGER")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `placeLeave` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `places` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                        "`latitude` REAL NOT NULL, `longitude` REAL NOT NULL)",
                )
            }
        }

        /** 0.15 → 0.16: order by hand (starting as the current "last edited" order) and the history of each note. */
        val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `notes` SET `position` = `updatedAt`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_versions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`noteId` INTEGER NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `savedAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_versions_noteId` ON `note_versions` (`noteId`)")
            }
        }

        /** 0.13 → 0.14: protected notes. */
        val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `locked` INTEGER NOT NULL DEFAULT 0")
            }
        }

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
