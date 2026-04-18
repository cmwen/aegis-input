package com.aegisinput.engine.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity(tableName = "user_dict")
data class UserDictEntry(
    @PrimaryKey val code: String,
    val word: String,
    val frequency: Int = 1,
    val lastUsed: Long = System.currentTimeMillis()
)

@Dao
interface UserDictDao {
    @Query("SELECT * FROM user_dict WHERE code = :code ORDER BY frequency DESC")
    suspend fun getEntries(code: String): List<UserDictEntry>

    @Query("SELECT * FROM user_dict WHERE code LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun searchByPrefix(prefix: String, limit: Int = 20): List<UserDictEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserDictEntry)

    @Query("UPDATE user_dict SET frequency = frequency + 1, lastUsed = :timestamp WHERE code = :code AND word = :word")
    suspend fun boost(code: String, word: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_dict WHERE lastUsed < :cutoff AND frequency < :minFreq")
    suspend fun pruneOldEntries(cutoff: Long, minFreq: Int = 2)
}
