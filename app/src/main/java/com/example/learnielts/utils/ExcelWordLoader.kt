// excel表中读取全词表，用于生成学习计划,LearningPlanScreen调用

package com.example.learnielts.utils

import android.content.Context
import android.util.Log
import org.apache.poi.ss.usermodel.WorkbookFactory

object ExcelWordLoader {

    fun loadWordsFromAsset(context: Context, assetPath: String): List<String> {
        return try {
            val inputStream = context.assets.open(assetPath)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val words = mutableListOf<String>()

            for (row in sheet) {
                val cell = row.getCell(0)
                if (cell != null) {
                    val word = cell.toString().trim().lowercase()
                    if (word.isNotBlank()) {
                        words.add(word)
                    }
                }
            }

            words
        } catch (e: Exception) {
            Log.e("调试", "❌ 读取失败: ${e.message}")
            emptyList()
        }
    }
}
