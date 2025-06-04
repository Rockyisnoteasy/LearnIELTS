// 提取中文释义部分的工具类，用于翻牌卡等模块,FlipCardScreen调用

package com.example.learnielts.utils

object ChineseDefinitionExtractor {

    fun extract(definition: String?): String? {
        if (definition.isNullOrBlank()) return null

        val start = definition.indexOf("中文释义：")
        if (start == -1) return null

        val startIdx = start + "中文释义：".length
        val endIdx = definition.indexOf("词性：", startIdx)
        return if (endIdx == -1) {
            definition.substring(startIdx).trim()
        } else {
            definition.substring(startIdx, endIdx).trim()
        }
    }
}
