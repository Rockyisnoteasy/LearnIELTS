// learnielts/data/model/Notification.kt
package com.example.learnielts.data.model

import com.google.gson.annotations.SerializedName

data class Notification(
    val id: Int,
    val type: String,
    val title: String,
    val summary: String?,
    @SerializedName("icon_url") val iconUrl: String?,
    val body: String?,
    @SerializedName("publish_date") val publishDate: String // 使用String接收
)