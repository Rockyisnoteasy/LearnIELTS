// 文件路径: learnielts/data/model/MasteredWordsResponse.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

data class MasteredWordsResponse(
    @SerializedName("words") val words: List<String>
)