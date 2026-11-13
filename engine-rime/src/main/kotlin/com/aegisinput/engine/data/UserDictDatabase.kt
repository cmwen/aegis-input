package com.aegisinput.engine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserDictEntry::class], version = 2, exportSchema = false)
abstract class UserDictDatabase : RoomDatabase() {

    abstract fun userDictDao(): UserDictDao

    companion object {
        @Volatile
        private var INSTANCE: UserDictDatabase? = null

        fun getInstance(context: Context): UserDictDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDictDatabase::class.java,
                    "aegis_user_dict.db"
                ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE user_dict_new (code TEXT NOT NULL, word TEXT NOT NULL, frequency INTEGER NOT NULL, lastUsed INTEGER NOT NULL, PRIMARY KEY(code, word))")
                        db.execSQL("INSERT INTO user_dict_new SELECT code, word, frequency, lastUsed FROM user_dict")
                        db.execSQL("DROP TABLE user_dict")
                        db.execSQL("ALTER TABLE user_dict_new RENAME TO user_dict")
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
