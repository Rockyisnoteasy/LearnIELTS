package com.example.learnielts.data.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DictionaryDao {

    @Query("SELECT * FROM dictionary")
    fun getAllSync(): List<WordEntryEntity>  // 同步版，供 ViewModel 初始加载用

    @Query("SELECT * FROM dictionary WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): WordEntryEntity?
}
