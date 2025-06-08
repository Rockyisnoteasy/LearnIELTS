package com.example.learnielts.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_words")
data class PlanWordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String
)
