package com.example.learnielts.data.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary")
    suspend fun getAll(): List<WordEntryEntity>  // ✅ 挂起函数

    @Query("SELECT * FROM dictionary WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): WordEntryEntity?
}

