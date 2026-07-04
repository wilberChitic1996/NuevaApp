package gt.guardian.cadejo.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app database. Version 1. Migrations are added explicitly as the schema
 * evolves — never `fallbackToDestructiveMigration` in release, so player progress
 * is never silently wiped. The exported schema (see ksp room.schemaLocation) lets
 * migrations be diffed and tested.
 */
@Database(
    entities = [ProfileEntity::class, RunHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CadejoDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun runHistoryDao(): RunHistoryDao

    companion object {
        const val NAME = "cadejo.db"
    }
}
