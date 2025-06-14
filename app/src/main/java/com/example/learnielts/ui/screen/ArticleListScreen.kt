// learnielts/ui/screen/ArticleListScreen.kt
package com.example.learnielts.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.learnielts.viewmodel.ArticleViewModel
import com.example.learnielts.data.model.ArticleSnippet
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage // 用于加载网络图片
import android.widget.Toast
import java.time.format.DateTimeFormatter // 用于格式化 LocalDate
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    articleViewModel: ArticleViewModel = viewModel(),
    onBack: () -> Unit,
    onArticleClick: (Int) -> Unit // 回调函数，用于导航到文章详情页，传入文章ID
) {
    val articles by articleViewModel.articleList.collectAsState() // 收集文章列表状态
    val isLoading by articleViewModel.isLoading.collectAsState() // 收集加载状态
    val errorMessage by articleViewModel.errorMessage.collectAsState() // 收集错误信息

    val context = LocalContext.current

    // 当此屏幕首次加载或 articleViewModel 实例变化时，获取文章列表
    LaunchedEffect(key1 = articleViewModel) {
        articleViewModel.fetchArticleList()
    }

    // 监听错误信息并显示Toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            articleViewModel.clearErrorMessage() // 清除错误信息，避免重复显示
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("精选阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                Text(
                    text = "暂无文章可供阅读。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(articles) { article ->
                        ArticleListItem(article = article, onClick = onArticleClick)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListItem(article: ArticleSnippet, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(article.id) }, // 点击卡片导航到详情页
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 封面图片 (如果存在)
            article.coverImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "文章封面",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp) // 固定高度
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop // 裁剪以填充空间
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 文章来源和日期
                Column {
                    article.source?.let {
                        Text(
                            text = "来源: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "发布日期: ${article.publishDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}", // 格式化日期
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                // 难度级别
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, // 或者选择一个更合适的图标
                        contentDescription = "难度",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "难度: ${article.difficultyLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}