package com.example.learnielts.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnielts.data.remote.ApiClient
import com.example.learnielts.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.learnielts.data.model.RegisterRequest
import android.widget.Toast


class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val prefs = context.getSharedPreferences("auth_prefs", 0)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    init {
        // 启动时尝试自动加载
        val savedToken = prefs.getString("access_token", null)
        if (!savedToken.isNullOrBlank()) {
            _token.value = savedToken
            viewModelScope.launch {
                try {
                    val user = ApiClient.authService.getProfile("Bearer $savedToken")
                    _profile.value = user
                    println("✅ 自动登录成功：${user.email}")
                } catch (e: Exception) {
                    println("❌ 自动登录失败：${e.message}")
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authService.login(
                    username = email,  // ✅ 用 email 填到 username 字段
                    password = password
                )
                _token.value = response.access_token

                prefs.edit().putString("access_token", response.access_token).apply() // ✅ 保存 token

                val profile = ApiClient.authService.getProfile("Bearer ${response.access_token}")
                _profile.value = profile
                Log.d("登录", "✅ 成功获取用户信息 ${profile.email}")
            } catch (e: Exception) {
                Log.e("登录", "❌ 登录失败: ${e.message}")
            }
        }
    }

    fun logout() {
        _token.value = null
        _profile.value = null
        prefs.edit().remove("access_token").apply()
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authService.register(
                    RegisterRequest(email, password)
                )
                Log.d("注册", "✅ 注册成功，尝试登录")

                // ✅ 注册成功提示
                Toast.makeText(context, "注册成功，正在登录…", Toast.LENGTH_SHORT).show()

                login(email, password)
            } catch (e: Exception) {
                Log.e("注册", "❌ 注册失败: ${e.message}")
                Toast.makeText(context, "注册失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



}

