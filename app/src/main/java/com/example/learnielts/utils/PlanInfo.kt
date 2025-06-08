package com.example.learnielts.utils

/**
 * 学习计划信息的数据结构，用于表示一个完整的学习计划配置。
 *
 * @property planName 用户自定义的学习计划名称（如“冲刺四级计划”）
 * @property category 所属分类（如“四六级”、“雅思”）
 * @property selectedPlan 所选择的单词表名称（如“四级深度记忆核心1905词”）
 * @property dailyCount 每天学习的单词数
 */
data class PlanInfo(
    val planName: String,
    val category: String,
    val selectedPlan: String,
    val dailyCount: Int
)

