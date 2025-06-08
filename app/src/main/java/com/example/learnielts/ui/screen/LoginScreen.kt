
package com.example.learnielts.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.learnielts.viewmodel.AuthViewModel


@Composable
fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }  // ✅ 登录 / 注册 切换
    val profile by viewModel.profile.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(if (isLoginMode) "登录" else "注册", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (isLoginMode) {
                    viewModel.login(email, password)
                } else {
                    viewModel.register(email, password)
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (isLoginMode) "登录" else "注册")
        }

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "没有账号？去注册" else "已有账号？去登录")
        }

        profile?.let {
            Text("欢迎，${it.email}")
            onLoginSuccess()
        }
    }
}

