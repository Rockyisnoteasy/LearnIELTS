// 加载 CSV 词典为词条数据结构,DictionaryViewModel调用
// 加载SQLite字典

package com.example.learnielts.data

import android.content.Context
import com.example.learnielts.data.room.AppDatabase
import com.example.learnielts.data.room.WordEntryEntity

object DictionaryLoader {
    fun loadDictionary(context: Context): List<WordEntry> {
        val dao = AppDatabase.getInstance(context).dictionaryDao()
        val entries: List<WordEntryEntity> = dao.getAllSync()

        return entries.map { WordEntry(it.word, it.definition) }
    }
}
