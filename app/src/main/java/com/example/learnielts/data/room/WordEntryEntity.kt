package com.example.learnielts.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "dictionary")
data class WordEntryEntity(
    @PrimaryKey val word: String,
    val definition: String,
    @ColumnInfo(name = "related_words") val relatedWords: String?
)
