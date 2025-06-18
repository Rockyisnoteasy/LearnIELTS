// learnielts/data/repository/NotificationRepository.kt
package com.example.learnielts.data.repository

import android.util.Log
import com.example.learnielts.data.model.Notification
import com.example.learnielts.data.remote.AuthService

class NotificationRepository(private val authService: AuthService) {
    suspend fun getNotifications(token: String, page: Int, limit: Int): List<Notification> {
        return try {
            authService.getNotifications("Bearer $token", page, limit)
        } catch (e: Exception) {
            Log.e("调试", "Failed to get notifications: ${e.message}", e)
            emptyList()
        }
    }
}

