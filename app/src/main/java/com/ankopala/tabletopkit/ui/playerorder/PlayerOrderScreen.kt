package com.ankopala.tabletopkit.ui.playerorder

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 排序玩家结果：座位号 1..N 依次出现，每个数字带呼吸放大缩小动画。
 *
 * 流程：
 * - 圆点先全部画出。
 * - 座位号数字按顺序（1 → 2 → 3 …）逐个出现，间隔约 450ms。
 * - 每个已出现的数字用呼吸缩放（infiniteTransition + per-seat 相位偏移）。
 */
@Composable
fun PlayerOrderScreen(
    ordered: List<PlayerOrderViewModel.OrderedPlayer>,
    onBack: () -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val seatTextColor = MaterialTheme.colorScheme.onSurface

    // 已显示的数量：逐帧递增，实现「依次显示」
    var shownCount by remember(ordered) { mutableIntStateOf(0) }

    LaunchedEffect(ordered) {
        shownCount = 0
        ordered.forEachIndexed { index, _ ->
            delay(320)
            shownCount = index + 1
        }
    }

    // 呼吸相位：所有数字共用一个连续相位，各数字按座位号偏移
    val breathPhase by rememberInfiniteTransition(label = "orderBreath").animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orderBreathPhase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            ordered.forEachIndexed { index, op ->
                val p = op.player
                val center = Offset(p.x * w, p.y * h)
                val r = 34.dp.toPx()
                drawCircle(color = p.color, radius = r, center = center)

                // 只有已显示到的座位号才画数字，并带呼吸缩放
                if (index < shownCount) {
                    val phase = breathPhase + (op.seat % 8) * (Math.PI / 4).toFloat()
                    val scale = 1f + 0.15f * kotlin.math.sin(phase).toFloat()
                    val layout = textMeasurer.measure(
                        text = op.seat.toString(),
                        style = TextStyle(
                            fontSize = (28 * scale).sp,
                            color = seatTextColor,
                            fontWeight = FontWeight.Black,
                        ),
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            center.x - layout.size.width / 2f,
                            center.y - layout.size.height / 2f,
                        ),
                    )
                }
            }
        }
    }
}
