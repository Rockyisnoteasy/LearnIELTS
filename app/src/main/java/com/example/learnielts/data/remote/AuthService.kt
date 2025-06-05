package com.example.learnielts.data.remote
import com.example.learnielts.data.model.TokenResponse
import com.example.learnielts.data.model.UserProfile
import retrofit2.Response
import com.example.learnielts.data.model.RegisterRequest
import retrofit2.http.*

interface AuthService {

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String
    ): UserProfile

    @POST("register")
    suspend fun register(
        @Body data: RegisterRequest
    ): Response<Unit>

}
