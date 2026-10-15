package com.ankopala.tabletopkit.ui.playerorder

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankopala.tabletopkit.data.Player
import kotlin.math.atan2
import kotlin.math.hypot

// 扩散缓动：先快后慢（ease-out），无回弹，模拟「甩出去」的惯性感
private val ExpandEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

// 回收缓动：先快后慢（ease-out），无回弹。
private val ReturnEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * 排序玩家结果展示 + 动画。
 *
 * 与起始玩家页共用同一套「同心圆环」动画模型：
 * - 圆环扩散（winner 同色、半透明）扫过其他圆点时擦除重合部分；
 * - 回收（背景色）时 winner 纯色从外向内铺，扫过的圆点在纯色上 DstOut 挖出「轮廓洞」；
 * - 圆环最终停在离 winner 圆点固定间隙处。
 *
 * 与起始玩家页唯一的区别：**其他玩家圆点显示座位号数字**（seat=2..N）。
 * - 数字颜色 = winner 圆点同色。
 * - 数字与圆点一样「扫到才显示」：回收纯色扫过该圆点时，数字才显现。
 * - winner（seat=1）本身不显示数字，作为中心圆点全程呼吸。
 */
@Composable
fun PlayerOrderScreen(
    ordered: List<PlayerOrderViewModel.OrderedPlayer>,
    dotRadiusDp: Float,
    onBack: () -> Unit,
) {
    // 首个座位（seat=1）即 winner，其余为「其他玩家」，需显示数字
    val winner = ordered.firstOrNull()?.player ?: return
    val others = ordered.drop(1)

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

    // 数字文本测量器
    val textMeasurer = rememberTextMeasurer()

    // 屏幕对角长度（px）
    var maxRadiusPx by remember { mutableFloatStateOf(0f) }

    // 圆环外半径（px），驱动扩散/回收
    val ringOuter = remember { Animatable(0f) }
    var returning by remember { mutableStateOf(false) }
    var filled by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background
    // 数字颜色 = winner 圆点同色
    val seatColor = winner.color

    LaunchedEffect(winner.id, maxRadiusPx, finalOuterRadiusPx) {
        if (maxRadiusPx <= 0f) return@LaunchedEffect
        returning = false

        filled = false
        ringOuter.snapTo(0f)
        // 扩散：0 → 屏幕对角，圆环为 winner 同色半透明
        ringOuter.animateTo(maxRadiusPx, tween(durationMillis = 1000, easing = ExpandEasing))
        // 回收：屏幕对角 → 终点半径，圆环为背景色
        returning = true
        ringOuter.animateTo(finalOuterRadiusPx, tween(durationMillis = 900, easing = ReturnEasing))
        returning = false
        filled = true
    }

    // 呼吸：无限循环，全程不停
    val breathScale by rememberInfiniteTransition(label = "orderBreath").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orderBreathScale",
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

        // 其他玩家圆点半径：默认全程呼吸；「被回收扫过的圆点」停止呼吸、定格基准大小。
        fun otherRadius(p: Player): Float {
            val dist = hypot(p.x * w - center.x, p.y * h - center.y)
            val scanned = (returning || filled) && dist >= outerR
            return if (scanned) baseRPx else baseRPx * breathScale
        }

        // 数字是否「扫到才显示」：回收/铺满阶段，圆点中心到 winner 中心距离 ≥ outerR。
        fun seatVisible(p: Player): Boolean {
            val dist = hypot(p.x * w - center.x, p.y * h - center.y)
            return (returning || filled) && dist >= outerR
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

                    others.forEach { op ->
                        drawCircle(
                            color = Color.Black,
                            radius = otherRadius(op.player),
                            center = Offset(op.player.x * w, op.player.y * h),
                            blendMode = BlendMode.DstOut,
                        )
                    }

                    canvas.restore()
                }
                // 背景色环带：在纯色上抠出一圈背景色间隙
                drawPath(path = ringPath(outerR), color = bgColor)

                // 数字：扫到才显示（winner 同色），画在轮廓洞之上
                others.forEach { op ->
                    if (seatVisible(op.player)) {
                        drawSeatNumber(op.seat, op.player, dotRadiusDp, seatColor, textMeasurer, w, h)
                    }
                }
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
                    others.forEach { op ->
                        drawCircle(
                            color = op.player.color,
                            radius = otherRadius(op.player),
                            center = Offset(op.player.x * w, op.player.y * h),
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
                // 可见的扩散前沿：winner 同色环带，画在最上层。
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
                others.forEach { op ->
                    drawCircle(
                        color = Color.Black,
                        radius = otherRadius(op.player),
                        center = Offset(op.player.x * w, op.player.y * h),
                        blendMode = BlendMode.DstOut,
                    )
                }
                canvas.restore()
            }
            drawPath(path = ringPath(outerR), color = bgColor)

            // 数字：铺满阶段全部显示（winner 同色）
            others.forEach { op ->
                drawSeatNumber(op.seat, op.player, dotRadiusDp, seatColor, textMeasurer, w, h)
            }
        }

        // ── 图层 3：winner 圆点（全程呼吸）──────────────────
        drawCircle(color = winner.color, radius = winnerR, center = center)
    }
}

/**
 * 在指定玩家圆点中心绘制座位号数字（winner 同色）。
 * 数字「头部朝屏幕中心」旋转——让围坐桌边的玩家都能正读自己的序号。
 *
 * 算法：以屏幕正中心为圆心，计算圆点到中心的方向角，数字旋转到「头朝中心」。
 * 数字大小随圆点半径缩放，使数字与圆点视觉协调。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeatNumber(
    seat: Int,
    player: Player,
    dotRadiusDp: Float,
    color: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    w: Float,
    h: Float,
) {
    val center = Offset(player.x * w, player.y * h)
    val screenCenterX = w / 2f
    val screenCenterY = h / 2f

    // 圆点相对屏幕中心的方向（中心 → 圆点）
    val dx = center.x - screenCenterX
    val dy = center.y - screenCenterY

    // 数字「头朝中心」的顺时针旋转角：
    // 默认文本顶部朝上（屏幕 -Y），要让顶部指向「圆点 → 屏幕中心」方向。
    // 屏幕坐标（Y 向下），角度 = atan2(-dx, dy)。
    //   正下方(dx=0,dy>0) → 0° 正向；正上方(dx=0,dy<0) → 180° 倒置；
    //   右侧(dx>0,dy=0) → -90° 头朝左。均与「头朝中心」吻合。
    val angleDeg = Math.toDegrees(atan2(-dx.toDouble(), dy.toDouble())).toFloat()

    // 数字字号：约为圆点直径的 0.55 倍，随圆点大小自适应（用 dp，避免密度差异）
    val fontSize = (dotRadiusDp * 1.1f).coerceAtLeast(14f)
    val layout = textMeasurer.measure(
        text = seat.toString(),
        style = TextStyle(
            fontSize = fontSize.sp,
            color = color,
            fontWeight = FontWeight.Black,
        ),
    )

    rotate(degrees = angleDeg, pivot = center) {
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                center.x - layout.size.width / 2f,
                center.y - layout.size.height / 2f,
            ),
        )
    }
}
