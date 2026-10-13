package com.ankopala.tabletopkit.core

import androidx.compose.ui.graphics.Color

/**
 * 玩家彩色圆点的固定色板（Material 500 系列，12 个高区分度颜色）。
 *
 * 需求要求：彩色圆点「不随主题变」，保证颜色识别不受深/浅主题影响。
 * 因此这里用固定常量，与 ui/theme 里的主题色（背景/文字/控件）语义分离。
 */
val PLAYER_PALETTE: List<Color> = listOf(
    Color(0xFFF44336), // Red 500
    Color(0xFFE91E63), // Pink 500
    Color(0xFF9C27B0), // Purple 500
    Color(0xFF673AB7), // Deep Purple 500
    Color(0xFF3F51B5), // Indigo 500
    Color(0xFF2196F3), // Blue 500
    Color(0xFF00BCD4), // Cyan 500
    Color(0xFF009688), // Teal 500
    Color(0xFF4CAF50), // Green 500
    Color(0xFFFF9800), // Orange 500
    Color(0xFFFFC107), // Amber 500
    Color(0xFF795548), // Brown 500
)
