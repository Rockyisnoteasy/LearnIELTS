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

    fun ultraSimplify(simplifiedDefinition: String?): String? {
        if (simplifiedDefinition.isNullOrBlank()) return null

        return try {
            simplifiedDefinition
                // 1. 使用分号分割成多个 "词性. 释义" 部分
                .split('；')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                // 2. 移除每个部分的词性前缀
                .map { it.replace(Regex("""^(n|v|adj|adv|prep|conj)\.\s*"""), "") }
                // 3. 过滤掉处理后为空的字符串
                .filter { it.isNotBlank() }
                // 4. 从有效释义中随机选择一个
                .randomOrNull()
        } catch (e: Exception) {
            // 如果发生异常，返回原始简化版作为降级方案
            simplifiedDefinition
        }
    }

}
