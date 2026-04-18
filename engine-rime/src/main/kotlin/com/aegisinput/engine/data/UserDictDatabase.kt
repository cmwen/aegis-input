package com.aegisinput.engine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserDictEntry::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
