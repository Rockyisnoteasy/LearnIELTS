/**
 * AuthViewModel.kt
 *
 * 该类是用户认证模块的核心 ViewModel，负责管理以下功能：
 *
 * ✅ 登录流程：调用 AuthService.login 接口，获取并保存 access_token；
 * ✅ 注册流程：调用 AuthService.register 接口，注册成功后自动登录；
 * ✅ 自动登录：在初始化时读取 SharedPreferences 中的 token 并尝试获取用户资料；
 * ✅ 学习计划同步：登录成功后，从服务器同步用户的学习计划数据。
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
import com.example.learnielts.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import org.json.JSONObject
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.example.learnielts.data.model.DailyWords
import com.example.learnielts.data.model.PlanCreateRequest
import com.example.learnielts.utils.PlanInfo
import kotlinx.coroutines.withContext

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val prefs = context.getSharedPreferences("auth_prefs", 0)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    // 用于存放学习计划列表的状态
    private val _plans = MutableStateFlow<List<PlanInfo>>(emptyList())
    val plans: StateFlow<List<PlanInfo>> = _plans

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
                    Log.d("调试", "✅ 自动登录成功：${user.email}")

                    // ✅ 自动登录成功后，也同步一次学习计划
                    syncPlans()

                } catch (e: Exception) {
                    Log.e("调试", "❌ 自动登录失败：${e.message}")
                    // 如果 token 无效，也应该登出
                    handleApiError(e, "自动登录")
                }
            }
        }
    }

    // 统一的API错误处理函数
    private fun handleApiError(e: Exception, fromFunction: String) {
        if (e is HttpException && e.code() == 401) {
            Log.d("调试", "❗ 在 '$fromFunction' 中检测到401，执行登出")
            logout()
            _toastMessage.value = "您的会话已过期或在其他设备登录"
        } else {
            Log.e("调试", "❌ 在 '$fromFunction' 中发生API错误: ${e.message}")
            // 可选：显示一个通用的错误提示
            // Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 从本地文件加载计划到 StateFlow 中
    fun loadPlans() {
        viewModelScope.launch(Dispatchers.IO) {
            _plans.value = FileHelper.loadAllPlans(context)
            Log.d("调试", "UI 状态已更新，当前计划数: ${_plans.value.size}")
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

                // ✅ 登录成功后，立即同步学习计划
                syncPlans()

                // ✅ 添加：启动10分钟一次的session校验
                startSessionCheckLoop()

            } catch (e: Exception) {
                Log.e("调试", "❌ 登录失败: ${e.message}")
                Toast.makeText(context, "登录失败，请检查账号或密码", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 从服务器同步学习计划的函数
    private suspend fun syncPlans() {
        val currentToken = "Bearer ${_token.value}"
        try {
            Log.d("调试", "🔄 开始从服务器同步学习计划...")
            val plansFromServer = ApiClient.authService.getPlans(currentToken)
            Log.d("调试", "✅ 从服务器获取到 ${plansFromServer.size} 个学习计划")

            // 使用 withContext 确保文件操作完成后再继续
            withContext(Dispatchers.IO) {
                FileHelper.overwriteLocalPlansFromServer(context, plansFromServer)
            }

            // ✅ 关键修正：操作完成后，调用 loadPlans() 刷新UI状态
            loadPlans()

        } catch (e: Exception) {
            handleApiError(e, "同步计划")
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

    // 创建新计划并上传
    fun createNewPlan(planName: String, category: String, selectedPlan: String, dailyCount: Int) {
        viewModelScope.launch {
            val currentToken = "Bearer ${_token.value}"
            try {
                // 1. 上传到服务器
                val request = PlanCreateRequest(planName, category, selectedPlan, dailyCount)
                val serverResponse = ApiClient.authService.createPlan(currentToken, request)
                Log.d("调试", "✅ 计划 '${planName}' 已成功上传到服务器, ID: ${serverResponse.id}")

                // 2. 更新本地文件
                val newPlanInfo = PlanInfo(
                    serverId = serverResponse.id,
                    planName = serverResponse.planName,
                    category = serverResponse.category,
                    selectedPlan = serverResponse.selectedPlan,
                    dailyCount = serverResponse.dailyCount
                )
                // ✅ --- 修改开始 ---
                // 使用 withContext 确保文件操作在IO线程完成
                withContext(Dispatchers.IO) {
                    // a. 将新计划信息保存到本地 current_plan.json
                    FileHelper.addPlanToCurrentList(context, newPlanInfo)

                    // b. 立刻为这个新计划生成今天的单词列表
                    Log.d("调试", "为新计划 '${planName}' 生成今日词表...")
                    val newWords = FileHelper.generateTodayWordListFromPlan(
                        context,
                        newPlanInfo.category,
                        newPlanInfo.selectedPlan,
                        newPlanInfo.planName,
                        newPlanInfo.dailyCount
                    )
                    Log.d("调试", "为新计划生成了 ${newWords.size} 个单词。")

                    // c. 如果生成了新单词，则调用后台上传（此函数内部会启动新协程，不会阻塞）
                    if (newWords.isNotEmpty()) {
                        uploadDailyWords(serverResponse.id, newWords)
                    }
                }

                // 3. 刷新UI，加载包含新计划的列表
                loadPlans()
                // ✅ --- 修改结束 ---

            } catch (e: Exception) {
                Log.e("调试", "❌ 创建新计划失败: ${e.message}")
                handleApiError(e, "创建新计划")
            }
        }
    }

    // ✅ 新增：删除计划并同步
    fun deletePlanOnServer(planInfo: PlanInfo) {
        val planId = planInfo.serverId ?: return
        viewModelScope.launch {
            val currentToken = "Bearer ${_token.value}"
            try {
                // 1. 从服务器删除
                ApiClient.authService.deletePlan(currentToken, planId)
                Log.d("调试", "✅ 计划 '${planInfo.planName}' 已从服务器删除")

                // 2. 从本地删除
                withContext(Dispatchers.IO) {
                    FileHelper.deletePlan(context, planInfo.planName)
                }
                loadPlans()
            } catch (e: Exception) {
                handleApiError(e, "删除计划")
            }
        }
    }

    // 上传每日单词列表
    fun uploadDailyWords(planId: Int, words: List<String>) {
        if (words.isEmpty()) return
        viewModelScope.launch {
            val currentToken = "Bearer ${_token.value}"
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyWords = DailyWords(wordDate = dateStr, words = words)
            try {
                ApiClient.authService.addDailyWords(currentToken, planId, dailyWords)
                Log.d("调试", "✅ 每日单词列表已上传, Plan ID: $planId, Date: $dateStr")
            } catch (e: Exception) {
                handleApiError(e, "上传每日单词")
            }
        }
    }

    fun startSessionCheckLoop() {
        viewModelScope.launch {
            while (true) {
                delay(60 * 1000L) // 每5分钟问询一次
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
                        handleApiError(e, "心跳检测")
                    }
                }
            }
        }
    }

    fun resetLogoutFlag() {
        _loggedOut.value = false
    }
}