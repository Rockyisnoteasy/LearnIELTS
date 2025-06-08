/**
 * AuthViewModel.kt
 *
 * 该类是用户认证模块的核心 ViewModel，负责管理以下功能：
 *
 * ✅ 登录流程：调用 AuthService.login 接口，获取并保存 access_token；
 * ✅ 注册流程：调用 AuthService.register 接口，注册成功后自动登录；
 * ✅ 自动登录：在初始化时读取 SharedPreferences 中的 token 并尝试获取用户资料；
 * ✅ 退出登录：清除 token 和用户信息；
 *
 * 内部使用 StateFlow 暴露 token 和 profile 状态给 UI 层；
 * token 存储在 SharedPreferences（文件名为 "auth_prefs"）中。
 */


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
import retrofit2.HttpException
import org.json.JSONObject
import kotlinx.coroutines.delay



class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val prefs = context.getSharedPreferences("auth_prefs", 0)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut



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
                Log.d("调试", "✅ 成功获取用户信息 ${profile.email}")

                // ✅ 添加：启动10分钟一次的session校验
                startSessionCheckLoop()

            } catch (e: Exception) {
                Log.e("调试", "❌ 登录失败: ${e.message}")
            }
        }
    }

    fun logout() {
        _token.value = null
        _profile.value = null
        prefs.edit().remove("access_token").apply()
        _loggedOut.value = true
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.authService.register(
                    RegisterRequest(email, password)
                )

                if (!response.isSuccessful) {
                    // ❌ 注册失败，读取后端返回的错误信息
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = org.json.JSONObject(errorBody ?: "")
                        json.optString("detail", "注册失败")
                    } catch (e: Exception) {
                        "注册失败"
                    }

                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("调试", "✅ 注册成功，尝试登录")
                Toast.makeText(context, "注册成功，正在登录…", Toast.LENGTH_SHORT).show()
                login(email, password)

            } catch (e: Exception) {
                Log.e("调试", "❌ 注册失败: ${e.message}")
                Toast.makeText(context, "注册失败：网络异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startSessionCheckLoop() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 每5分钟问询一次
                val token = prefs.getString("access_token", null)
                if (!token.isNullOrBlank()) {
                    try {
                        Log.d("调试", "发送心跳请求中...")
                        val result = ApiClient.authService.checkSession("Bearer $token")
                        Log.d("调试", "心跳返回：ok=${result.ok}")
                        if (!result.ok) {
                            logout()
                            _toastMessage.value = "您的账号已在其他设备登录"
                            Log.d("调试", "执行 logout()，因为 session 无效")
                        }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 401) {
                            Log.d("调试", "❗ 发现 token 被服务器判定为失效，执行 logout()")
                            logout()
                            _toastMessage.value = "您的账号已在其他设备登录"
                        } else {
                            Log.e("调试", "❌ Http 错误：${e.code()}，跳过")
                        }
                    } catch (e: Exception) {
                        Log.d("调试", "⚠️ 无法连接服务器，跳过本次验证：${e.message}")
                    }
                }
            }
        }
    }

    fun resetLogoutFlag() {
        _loggedOut.value = false
    }
}

