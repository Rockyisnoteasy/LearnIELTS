// 加载 CSV 词典为词条数据结构,DictionaryViewModel调用
// 加载SQLite字典

package com.example.learnielts.data

import android.util.Log
import android.content.Context
import com.example.learnielts.data.room.AppDatabase
import com.example.learnielts.data.room.WordEntryEntity

object DictionaryLoader {
    suspend fun loadDictionary(context: Context): List<WordEntry> {
        return try {
            Log.d("调试", "📥 开始加载词典...")
            val dao = AppDatabase.getInstance(context).dictionaryDao()
            val entries = dao.getAll() // ✅ 用挂起函数 getAll()
            Log.d("调试", "✅ 词典加载完成，共 ${entries.size} 项")
            entries.map { WordEntry(it.word, it.definition, it.relatedWords) }
        } catch (e: Exception) {
            Log.e("调试", "❌ 加载词典失败：${e.message}", e)
            emptyList()
        }
    }

}

