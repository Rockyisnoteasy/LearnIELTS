// 加载 CSV 词典为词条数据结构,DictionaryViewModel调用

package com.example.learnielts.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import com.opencsv.CSVReader


object DictionaryLoader {
    fun loadDictionary(context: Context): List<WordEntry> {
        val entries = mutableListOf<WordEntry>()
        val inputStream = context.assets.open("custom_dictionary.csv")
        val reader = CSVReader(InputStreamReader(inputStream))

        reader.use {
            val allRows = reader.readAll()
            for (i in 1 until allRows.size) {  // 跳过表头
                val row = allRows[i]
                if (row.size >= 2) {
                    val word = row[0].trim()
                    val definition = row[1].trim()
                    entries.add(WordEntry(word, definition))
                }
            }
        }

        return entries
    }
}
