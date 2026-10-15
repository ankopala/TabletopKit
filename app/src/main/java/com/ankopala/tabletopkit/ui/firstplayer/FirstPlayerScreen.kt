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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.data.Player
import kotlin.math.hypot

// 扩散缓动：先快后慢（ease-out），无回弹，模拟「甩出去」的惯性感
private val ExpandEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

// 回收缓动：先快后慢（ease-out），无回弹。
private val ReturnEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * 起始玩家结果展示 + 动画。
 *
 * 核心模型：一个与 winner 圆点「同心」的**圆环**（环带，环宽固定）。
 * 颜色随阶段切换：扩散 = winner 同色，回收 = 主题背景色。
 *
 * 核心原则：**遮罩由「圆环实际碰到」触发**，碰到才遮、没碰到不遮。
 *
 * 动画流程：
 * 1. 扩散阶段（圆环 = winner 同色、半透明）：圆环向外扩散，扫过其他玩家圆点时，
 *    把「碰到的部分」淡化（半透明环带盖住圆点重合部分，原色变淡，但不消失）。
 * 2. 回收阶段（圆环 = 背景色）：winner 纯色从外向内铺（前沿 = 环带外半径）；
 *    纯色扫过其他玩家圆点时，该圆点在纯色上 DstOut 挖出「圆点形状的洞」，
 *    洞里露出背景色轮廓——透过纯色能「看见它们原来的位置」。
 * 3. 圆环回收终点停在离 winner 圆点固定间隙（ringGap）处，不融为一体。
 *
 * 呼吸（**全程不停，贯穿扩散与回收**）：
 * - 所有圆点（winner + 其他玩家）无条件随 breathScale 呼吸，与阶段无关。
 * - 动画结束后（filled），圆环再与 winner 反向呼吸；动画中圆环半径由 ringOuter 独立驱动。
 */
@Composable
fun FirstPlayerScreen(
    winner: Player,
    others: List<Player>,
    settled: Boolean,
    dotRadiusDp: Float,
    onSettled: () -> Unit,
    onBack: () -> Unit,
) {
    val baseR = dotRadiusDp.dp
    // 圆环本身的宽度（内外半径之差）
    val ringWidth = 16.dp
    // 回收终点：圆环外半径 = winner 圆点 + 固定间隙
    val ringGap = 16.dp
    val finalOuterRadiusDp = baseR + ringGap

    val density = LocalDensity.current
    val ringWidthPx = with(density) { ringWidth.toPx() }
    val finalOuterRadiusPx = with(density) { finalOuterRadiusDp.toPx() }

    // saveLayer 合成 paint
    val layerPaint = remember { Paint() }

    // 屏幕对角长度（px）
    var maxRadiusPx by remember { mutableFloatStateOf(0f) }

    // 圆环外半径（px），驱动扩散/回收
    val ringOuter = remember { Animatable(0f) }
    var returning by remember { mutableStateOf(false) }
    var filled by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background

    LaunchedEffect(winner.id, maxRadiusPx, finalOuterRadiusPx) {
        if (maxRadiusPx <= 0f) return@LaunchedEffect
        returning = false

        if (settled) {
            ringOuter.snapTo(finalOuterRadiusPx)
            filled = true
            return@LaunchedEffect
        }

        filled = false
        ringOuter.snapTo(0f)
        // 扩散：0 → 屏幕对角，圆环为 winner 同色半透明
        ringOuter.animateTo(maxRadiusPx, tween(durationMillis = 1000, easing = ExpandEasing))
        // 回收：屏幕对角 → 终点半径，圆环为背景色
        returning = true
        ringOuter.animateTo(finalOuterRadiusPx, tween(durationMillis = 900, easing = ReturnEasing))
        returning = false
        filled = true
        onSettled()
    }

    // 呼吸：无限循环，全程不停
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
            .background(bgColor)
            .onSizeChanged { size ->
                maxRadiusPx = hypot(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(winner.x * w, winner.y * h)
        val baseRPx = baseR.toPx()

        // winner 圆点：全程呼吸
        val winnerR = baseRPx * breathScale

        // 圆环外半径：
        // - 动画阶段（扩散/回收）：由 ringOuter 独立驱动。
        // - 结束阶段（filled）：与 winner 反向呼吸（winner 放大 → 环缩小）。
        val outerR = if (filled) {
            finalOuterRadiusPx * (2f - breathScale)
        } else {
            ringOuter.value
        }

        // 其他玩家圆点半径：默认全程呼吸；但「被回收扫过的圆点」停止呼吸、定格基准大小。
        // 判断「被扫过」：回收/铺满阶段，圆点中心到 winner 中心的距离 ≥ 当前纯色前沿（outerR）。
        // 回收是单调的（outerR 递减），一旦满足即永久成立，故天然「扫过即停」。
        fun otherRadius(p: Player): Float {
            val dist = hypot(p.x * w - center.x, p.y * h - center.y)
            val scanned = (returning || filled) && dist >= outerR
            return if (scanned) baseRPx else baseRPx * breathScale
        }

        // 环带路径：大圆 - 内圆（even-odd）
        fun ringPath(outer: Float): Path {
            val r = outer.coerceAtLeast(0f)
            val inner = (r - ringWidthPx).coerceAtLeast(0f)
            return Path().apply {
                fillType = PathFillType.EvenOdd
                addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
                if (inner > 0f) {
                    addOval(Rect(center.x - inner, center.y - inner, center.x + inner, center.y + inner))
                }
            }
        }

        // ── 图层 1 + 2：其他玩家圆点 + 遮罩（saveLayer 隔离）──
        if (!filled) {
            if (returning) {
                // 回收阶段：先铺 winner 纯色（从环带外边缘到屏幕边缘），
                // 再用其他玩家圆点 DstOut 挖洞，露出背景色轮廓（透过纯色看见原位置）。
                drawIntoCanvas { canvas ->
                    val saveCount = canvas.saveLayer(
                        bounds = Rect(0f, 0f, w, h),
                        paint = layerPaint,
                    )
                    val filledPath = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addOval(
                            Rect(
                                center.x - maxRadiusPx,
                                center.y - maxRadiusPx,
                                center.x + maxRadiusPx,
                                center.y + maxRadiusPx,
                            )
                        )
                        val r = outerR.coerceAtLeast(0f)
                        addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
                    }
                    drawPath(path = filledPath, color = winner.color)

                    others.forEach { p ->
                        drawCircle(
                            color = Color.Black,
                            radius = otherRadius(p),
                            center = Offset(p.x * w, p.y * h),
                            blendMode = BlendMode.DstOut,
                        )
                    }

                    canvas.restore()
                }
                // 背景色环带：在纯色上抠出一圈背景色间隙
                drawPath(path = ringPath(outerR), color = bgColor)
            } else {
                // 扩散阶段：其他玩家圆点（原色、呼吸）为「目标」，
                // 用「实心圆」（半径 = 环带外半径）作为 DstOut 蒙版擦除——
                // 圆环扫过的区域，圆点被永久挖掉（露出背景色），不再复活。
                drawIntoCanvas { canvas ->
                    val saveCount = canvas.saveLayer(
                        bounds = Rect(0f, 0f, w, h),
                        paint = layerPaint,
                    )
                    // 目标：其他玩家圆点（原色、全程呼吸）
                    others.forEach { p ->
                        drawCircle(
                            color = p.color,
                            radius = otherRadius(p),
                            center = Offset(p.x * w, p.y * h),
                        )
                    }
                    // 源：实心圆蒙版（半径 = 环带外半径），DstOut 擦除圆点
                    val r = outerR.coerceAtLeast(0f)
                    drawCircle(
                        color = Color.Black,
                        radius = r,
                        center = center,
                        blendMode = BlendMode.DstOut,
                    )
                    canvas.restore()
                }
                // 可见的扩散前沿：winner 同色环带，画在最上层，
                // 让扩散过程「看得到」圆环在往外扩（扫过的圆点已被擦除露背景色）。
                drawPath(path = ringPath(outerR), color = winner.color)
            }
        } else {
            // 铺满阶段：全屏 winner 纯色 + 圆点挖洞（轮廓持续可见）+ 背景色环带
            drawIntoCanvas { canvas ->
                val saveCount = canvas.saveLayer(
                    bounds = Rect(0f, 0f, w, h),
                    paint = layerPaint,
                )
                drawRect(color = winner.color, topLeft = Offset.Zero, size = Size(w, h))
                others.forEach { p ->
                    drawCircle(
                        color = Color.Black,
                        radius = otherRadius(p),
                        center = Offset(p.x * w, p.y * h),
                        blendMode = BlendMode.DstOut,
                    )
                }
                canvas.restore()
            }
            drawPath(path = ringPath(outerR), color = bgColor)
        }

        // ── 图层 3：winner 圆点（全程呼吸）──────────────────
        drawCircle(color = winner.color, radius = winnerR, center = center)
    }
}
