/**
 * ApiClient.kt
 *
 * 封装 Retrofit 客户端初始化逻辑，暴露单例对象 authService。
 *
 * ✅ BASE_URL 设置为你的服务器地址：https://api.savanalearns.fun/
 * ✅ 提供 Retrofit 实例，并绑定 AuthService 接口；
 * ✅ 使用 GsonConverterFactory 自动解析 JSON；
 *
 * 该文件统一管理所有网络服务入口，避免多处重复创建 Retrofit 实例。
 */


package com.example.learnielts.data.remote

import com.example.learnielts.data.remote.LocalDateAdapter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.time.LocalDate

object ApiClient {
    private const val BASE_URL = "https://api.savanalearns.fun/"

    // ✅ 1. 创建一个自定义的 Gson 实例
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter()) // ✅ 2. 注册我们的适配器
        .create()

    val authService: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)) // ✅ 3. 使用自定义的 Gson 实例
            .build()
            .create(AuthService::class.java)
    }
}
