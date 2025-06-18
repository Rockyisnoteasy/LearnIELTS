// learnielts/viewmodel/NotificationViewModel.kt
package com.example.learnielts.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.learnielts.data.model.Notification
import com.example.learnielts.data.remote.ApiClient
import com.example.learnielts.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    application: Application,
    private val authViewModel: AuthViewModel
) : AndroidViewModel(application) {

    private val repository = NotificationRepository(ApiClient.authService)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    fun fetchNotifications() {
        viewModelScope.launch {
            val token = authViewModel.token.value ?: return@launch
            val result = repository.getNotifications(token, 1, 20)
            _notifications.value = result
        }
    }
}

class NotificationViewModelFactory(
    private val application: Application,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(application, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

