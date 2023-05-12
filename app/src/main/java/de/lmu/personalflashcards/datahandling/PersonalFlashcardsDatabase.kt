package de.lmu.personalflashcards.datahandling

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.lmu.personalflashcards.model.LogDao
import de.lmu.personalflashcards.model.LogEntry
import de.lmu.personalflashcards.model.QuizDao
import de.lmu.personalflashcards.model.Quiz

@Database(entities = [Quiz::class, LogEntry::class], version = 2, exportSchema = true)
abstract class PersonalFlashcardsDatabase: RoomDatabase() {

    abstract fun quizDao(): QuizDao
    abstract fun logDao(): LogDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: PersonalFlashcardsDatabase? = null

        fun getDatabase(context: Context): PersonalFlashcardsDatabase {
            val tempInstance =
                INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PersonalFlashcardsDatabase::class.java,
                    "quiz_database")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `log` ADD `synced` INTEGER NOT NULL DEFAULT 0")
            }

        }
    }
}