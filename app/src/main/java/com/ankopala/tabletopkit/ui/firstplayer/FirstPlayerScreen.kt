package com.ankopala.tabletopkit.ui.firstplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.data.Player
import kotlin.math.hypot

// 扩散缓动：先快后慢（ease-out），无回弹，模拟「甩出去」的惯性感
private val ExpandEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

// 回收缓动：先快后慢（ease-out），无回弹。
private val ReturnEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/** 圆点半径（报名/结果统一） */
const val DOT_RADIUS_DP = 34

/**
 * 起始玩家结果展示 + 动画。
 *
 * 动画流程：
 * 1. 被选中的圆点向外扩散出一个圆环，一直扩散到屏幕边缘之外。
 * 2. 圆环返回（回收）时，圆环「带过」的屏幕区域（圆环之外、直到屏幕边缘）
 *    被起始玩家的颜色铺满，圆环就是「纯色覆盖到哪」的前沿边界。
 * 3. 回收结束：圆点之外几乎全屏都是起始玩家颜色，其他玩家圆点被盖住，
 *    起始玩家圆点用白色描边高亮浮在最上层，最突出。
 * 4. 圆点保持悬浮呼吸效果。
 *
 * 实现：用 [Animatable] 驱动「圆环半径」（px）。
 * 扩散 baseR → 屏幕对角；回收 → 圆点外圈半径。
 * 回收阶段用 Path(even-odd) 画「大圆挖去小圆」的环形，填 winner 颜色。
 */
@Composable
fun FirstPlayerScreen(
    winner: Player,
    others: List<Player>,
    settled: Boolean,
    onSettled: () -> Unit,
    onBack: () -> Unit,
) {
    val baseR = DOT_RADIUS_DP.dp
    // 纯色环与圆点之间的间隙：回收时纯色环停在离圆点这个距离的位置，
    // 必须大于呼吸动画的放大余量（34dp × 1.12 ≈ 38dp），才能始终留出清晰空白环。
    val ringGap = 12.dp
    val finalRingRadiusDp = baseR + ringGap

    // 提前把「回收终点半径」从 Dp 转成 px（LaunchedEffect 里不能调用 @Composable）
    val finalRingRadiusPx = with(LocalDensity.current) { finalRingRadiusDp.toPx() }

    // 屏幕对角长度（px），用来确定扩散的最大半径
    var maxRadiusPx by remember { mutableFloatStateOf(0f) }

    val ringRadius = remember { Animatable(0f) }
    var breathing by remember { mutableStateOf(false) }
    // 是否正处于「回收」阶段（扩散阶段不铺色，回收阶段才铺色）
    var returning by remember { mutableStateOf(false) }
    // 是否已铺满全屏（回收结束后持续保持填充色，不再消失）
    var filled by remember { mutableStateOf(false) }

    LaunchedEffect(winner.id, maxRadiusPx, finalRingRadiusPx) {
        if (maxRadiusPx <= 0f) return@LaunchedEffect
        breathing = false
        returning = false

        if (settled) {
            // 结果动画已播过（重新进入界面）：直接跳到最终态，不重播
            ringRadius.snapTo(finalRingRadiusPx)
            filled = true
            breathing = true
            return@LaunchedEffect
        }

        filled = false
        ringRadius.snapTo(0f)
        // 扩散：0 → 屏幕对角（冲出屏外）
        ringRadius.animateTo(maxRadiusPx, tween(durationMillis = 1000, easing = ExpandEasing))
        // 回收：屏幕对角 → 圆点外圈半径，此阶段铺色
        returning = true
        ringRadius.animateTo(finalRingRadiusPx, tween(durationMillis = 900, easing = ReturnEasing))
        returning = false
        filled = true   // 回收结束，全屏铺色持续停留
        breathing = true
        onSettled()     // 通知 ViewModel：动画已播完，之后重进不再重播
    }

    val breathScale by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { size ->
                maxRadiusPx = hypot(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(winner.x * w, winner.y * h)
        val baseRPx = baseR.toPx()

        // 其它玩家淡化为低透明度（铺色后会被 winner 纯色盖住，只在铺色前显示）
        if (!filled) {
            others.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = 0.3f),
                    radius = baseRPx,
                    center = Offset(p.x * w, p.y * h),
                )
            }
        }

        // 1) 回收阶段：圆环之外直到屏幕边缘，铺满 winner 纯色（不透明）
        //    用 Path even-odd 画「大圆 - 小圆」的环形填充。
        if (returning) {
            val filledPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // 外圆：半径取 maxRadius，保证覆盖整个屏幕
                addOval(Rect(center.x - maxRadiusPx, center.y - maxRadiusPx, center.x + maxRadiusPx, center.y + maxRadiusPx))
                // 内圆：半径 = 当前圆环半径（挖空，露出圆点一侧的原始画面）
                val r = ringRadius.value.coerceAtLeast(0f)
                addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
            }
            drawPath(path = filledPath, color = winner.color)
        }

        // 1.5) 回收结束后：全屏持续铺满 winner 纯色（不消失），盖住其他玩家圆点
        if (filled) {
            drawRect(
                color = winner.color,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, h),
            )
        }

        // 2) 扩散阶段：纯圆环线（无填充）
        if (!returning && !breathing && ringRadius.value > 0f) {
            drawCircle(
                color = winner.color,
                radius = ringRadius.value,
                center = center,
                style = Stroke(width = 10.dp.toPx()),
            )
        }

        // 3) 圆点自身（呼吸阶段半径随 breathScale 浮动）
        val r = if (breathing) baseRPx * breathScale else baseRPx
        drawCircle(color = winner.color, radius = r, center = center)

        // 4) 高亮：圆点外圈一圈白色描边（铺色后与纯色背景形成对比，突出起始玩家）
        //    仅在回收接近终点（圆环快收拢到圆点）或已结束时出现，避免回收一开始就提前冒出白环。
        val highlightRadius = r + 6.dp.toPx()
        val returnNearlyDone = returning && ringRadius.value < finalRingRadiusPx * 1.5f
        if (breathing || returnNearlyDone || filled) {
            drawCircle(
                color = Color.White,
                radius = highlightRadius,
                center = center,
                style = Stroke(width = 5.dp.toPx()),
            )
        }
    }
}
