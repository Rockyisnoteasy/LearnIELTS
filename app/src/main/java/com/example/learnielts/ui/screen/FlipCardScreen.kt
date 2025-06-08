// 翻牌学习页面，显示单词正反面，用于记忆练习（接收传入的词表），MainActivity调用
package com.example.learnielts.ui.screen

import androidx.activity.compose.BackHandler
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.learnielts.viewmodel.DictionaryViewModel
import com.wajahatkarim.flippable.Flippable
import com.wajahatkarim.flippable.rememberFlipController
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun FlipCardScreen(
    context: Context,
    wordList: List<String>,
    viewModel: DictionaryViewModel,
    onExit: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var isFront by remember { mutableStateOf(true) }
    var isFlipping by remember { mutableStateOf(false) }

    val flipController = rememberFlipController()
    val scope = rememberCoroutineScope()

    val word = wordList.getOrNull(currentIndex) ?: return onExit()
    val fullDef = viewModel.getDefinition(word) ?: "（无释义）"
    val shortDef = remember(word) {
        val start = fullDef.indexOf("中文释义：")
        val end = fullDef.indexOf("词性：")
        if (start != -1 && end != -1 && end > start) {
            fullDef.substring(start + 5, end).trim()
        } else fullDef
    }

    BackHandler {
        onExit()
    }


    fun moveToNextWord() {
        currentIndex++
        if (currentIndex >= wordList.size) {
            onExit()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部进度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${currentIndex + 1}/${wordList.size}", style = MaterialTheme.typography.bodyMedium)
            Text("熟悉度：○", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 翻牌卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Flippable(
                modifier = Modifier.fillMaxSize(),
                frontSide = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF2B2B2B))
                            .clickable {
                                if (!isFlipping) {
                                    isFlipping = true
                                    flipController.flip()
                                    isFront = false
                                    expanded = false
                                    scope.launch {
                                        delay(500)
                                        isFlipping = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                backSide = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF2B2B2B))
                            .clickable {
                                if (!isFlipping) {
                                    isFlipping = true
                                    flipController.flip()
                                    isFront = true
                                    expanded = false
                                    scope.launch {
                                        delay(500)
                                        isFlipping = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = if (expanded) fullDef else shortDef,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (!expanded) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "↓ 展开全部释义",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = true }
                                )
                            }
                        }
                    }
                },
                flipController = flipController,
                flipOnTouch = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 底部按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconTextButton(
                icon = Icons.Default.VolumeUp,
                label = "朗读",
                onClick = {
                    scope.launch { viewModel.playWord(word, context) }
                }
            )
            IconTextButton(
                icon = Icons.Default.Flip,
                label = "翻面",
                onClick = {
                    if (!isFlipping) {
                        isFlipping = true
                        flipController.flip()
                        isFront = !isFront
                        expanded = false
                        scope.launch {
                            delay(500)
                            isFlipping = false
                        }
                    }
                }
            )
            IconTextButton(
                icon = Icons.Default.ArrowForward,
                label = "继续",
                onClick = {
                    if (!isFlipping) {
                        isFlipping = true
                        expanded = false

                        scope.launch {
                            // 如果当前是背面，先翻回正面（但不等待动画完成）
                            if (!isFront) {
                                flipController.flip()
                                isFront = true
                                // 不等待翻转动画完成，立即切换到下一个单词
                                delay(500) // 只是给一个很小的延迟确保状态更新
                            }

                            // 切换到下一个单词
                            currentIndex++
                            if (currentIndex >= wordList.size) {
                                onExit()
                            }

                            // 确保下一个单词显示正面
                            isFront = true
                            isFlipping = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun IconTextButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A90E2))
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
