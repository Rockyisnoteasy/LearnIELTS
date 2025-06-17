package com.example.learnielts.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.learnielts.data.model.ArticleSnippet
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReadCard(
    article: ArticleSnippet,
    onStartRead: (Int) -> Unit,
    onLookBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Section: Brand Title and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Daily Reads",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = article.publishDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = article.publishDate.dayOfMonth.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = article.publishDate.year.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Log.d("调试", "DailyReadCard 接收到的 Article: $article")

            // Main Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Text Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 3
                    )
                    article.subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))

                // Right: Thumbnail Image (using thumbnail_url)
                article.thumbnailUrl?.let { imageUrl ->
                    Log.d("调试", "准备加载缩略图，URL: $imageUrl")
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = article.title,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Summary/Editor's note
            article.summary?.let {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "编辑推荐",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        maxLines = 3
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }


            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLookBack) {
                    Text("回看往期")
                }
                Button(onClick = { onStartRead(article.id) }) {
                    Text("开始阅读")
                }
            }
        }
    }
}