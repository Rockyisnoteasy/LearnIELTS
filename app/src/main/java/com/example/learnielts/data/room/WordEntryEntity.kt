package com.example.learnielts.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary")
data class WordEntryEntity(
    @PrimaryKey val word: String,
    val definition: String
)
