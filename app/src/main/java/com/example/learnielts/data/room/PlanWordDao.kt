package com.example.learnielts.data.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface PlanWordDao {

    // 返回的是所有单词的 List<String>，用于获取实际单词内容
    @Query("SELECT word FROM plan_words")
    suspend fun getAllWords(): List<String>

    @Query("""
        SELECT word FROM plan_words
        WHERE word NOT IN (:learnedWords)
        ORDER BY RANDOM()
        LIMIT :count
    """)
    suspend fun getUnlearnedWords(learnedWords: List<String>, count: Int): List<String>

    // 返回单词总数的 Int，用于日志记录、进度计算、动态分配等目的
    @Query("SELECT COUNT(*) FROM plan_words")
    suspend fun countAllWords(): Int

}
