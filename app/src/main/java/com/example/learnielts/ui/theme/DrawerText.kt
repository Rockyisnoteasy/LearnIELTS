package com.example.learnielts.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DrawerText(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val content = @Composable {
        Text(
            text = text,
            color = Color(0xFF52616B), // 统一抽屉文字颜色
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(16.dp)
        )
    }

    if (onClick != null) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
            content()
        }
    } else {
        content()
    }
}
