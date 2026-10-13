package com.ankopala.tabletopkit.data

import androidx.compose.ui.graphics.Color

/**
 * 一个玩家（报名时点下的一个圆点）。
 *
 * 坐标 [x]/[y] 采用归一化 0..1 的「相对坐标」：
 * - 存储时：手指落点像素位置 ÷ 采集区尺寸。
 * - 渲染时：归一化坐标 × 当前 Canvas 尺寸，还原为像素。
 *
 * 好处：屏幕旋转、换分辨率时 ViewModel 存活但像素坐标会错位，
 * 归一化后天然适配任意尺寸，无需在 ViewModel 里感知像素。
 *
 * [color] 是固定色板里分配的颜色（不随主题变）。
 */
data class Player(
    val id: Long,
    val x: Float,
    val y: Float,
    val color: Color,
)
