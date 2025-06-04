// PopupUtils.kt
// 通用函数，功能：点击单词，弹出释义框
// 弹出释义窗口，用于点击单词显示详情,WordListScreen调用
package com.example.learnielts.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.LinearLayout
import com.example.learnielts.viewmodel.DictionaryViewModel


fun showDefinitionPopup(
    context: Context,
    word: String,
    definition: String,
    viewModel: DictionaryViewModel
) {
    val scrollView = ScrollView(context)
    val outerLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 40, 40, 40)
    }

    // 第一行：单词标题 + 播放按钮
    val titleLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    val titleText = TextView(context).apply {
        text = "【$word】"
        textSize = 18f
    }

    val playButton = Button(context).apply {
        text = "🔊"
        textSize = 14f
        setPadding(20, 0, 20, 0)
        setOnClickListener {
            viewModel.playWord(word, context)
        }
    }

    titleLayout.addView(titleText)
    titleLayout.addView(playButton)

    // 第二部分：释义正文
    val definitionText = TextView(context).apply {
        text = "\n$definition"
        textSize = 16f
    }

    // 添加到整体布局中
    outerLayout.addView(titleLayout)
    outerLayout.addView(definitionText)
    scrollView.addView(outerLayout)

    AlertDialog.Builder(context)
        .setTitle("释义")
        .setView(scrollView)
        .setPositiveButton("关闭", null)
        .show()
}


