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

    fun simplify(definition: String?): String? {
        val raw = extract(definition) ?: return null
        return raw
            .split(Regex("""\n|\r|\d+[\.\、]""")) // 按换行或序号分割
            .mapNotNull { line ->
                line.trim()
                    .replace(Regex("（.*?）|\\(.*?\\)"), "") // 删除括号及其内容
                    .split(Regex("[,，；;]"))
                    .firstOrNull()
            }
            .filter { it.isNotBlank() }
            .joinToString("；")
    }

}
