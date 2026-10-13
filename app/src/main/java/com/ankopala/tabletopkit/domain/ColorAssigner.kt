package com.ankopala.tabletopkit.domain

import androidx.compose.ui.graphics.Color
import com.ankopala.tabletopkit.core.PLAYER_PALETTE

/**
 * 颜色分配器：为每个新报名的玩家分配一个颜色。
 *
 * 规则（需求要求）：
 * - 同一局内尽量不重复：从色板里随机取一个「还没被用过」的颜色。
 * - 色板用尽后复用：当所有颜色都用过一遍，则从整个色板随机取。
 */
class ColorAssigner(
    private val palette: List<Color> = PLAYER_PALETTE,
) {
    fun next(usedColors: Set<Color>): Color {
        val unused = palette.filter { it !in usedColors }
        return if (unused.isNotEmpty()) unused.random()
        else palette.random()
    }
}
