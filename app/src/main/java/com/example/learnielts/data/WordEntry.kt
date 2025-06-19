// 词条数据结构类，包含单词与释义等信息,DictionaryLoader调用


package com.example.learnielts.data

data class WordEntry(
    val word: String,
    val definition: String,
    val relatedWords: String?
)
