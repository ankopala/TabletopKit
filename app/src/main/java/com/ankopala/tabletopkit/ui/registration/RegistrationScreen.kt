package com.ankopala.tabletopkit.ui.registration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.data.ConfirmationMode
import com.ankopala.tabletopkit.data.Player
import kotlin.math.sqrt

/**
 * 报名采集通用 UI（三个工具共用）。
 *
 * 行为：
 * - 整个屏幕是一块可点击区域（Canvas）。
 * - 每个玩家轮流点一下，手指「落下」瞬间在落点记录一个圆点（用 awaitFirstDown 而非抬手）。
 * - 采集阶段结束后（由 [uiState.phase] 决定），显示各工具自己的结果。
 *
 * 采集阶段顶部有「返回 / 重开」入口；底部显示报名人数 + 一个「圆环逐渐缩短到空」的倒计时圈圈。
 */
@Composable
fun RegistrationScreen(
    uiState: RegistrationUiState,
    countdownProgress: Float,
    timeoutSeconds: Float,
    confirmationMode: ConfirmationMode,
    dotRadiusDp: Float,
    finishLabel: String,
    onTap: (normalizedX: Float, normalizedY: Float, timeoutSeconds: Float) -> Unit,
    onDragMove: (id: Long, normalizedX: Float, normalizedY: Float, timeoutSeconds: Float) -> Unit,
    onDragEnd: (timeoutSeconds: Float) -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    resultContent: @Composable () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val collecting = uiState.phase == RegistrationPhase.Collecting

        // 让手势 lambda 始终拿到最新的 players（避免 pointerInput 捕获过期状态导致命中检测不准）
        val currentPlayers by rememberUpdatedState(uiState.players)

        // 全局呼吸相位：驱动所有圆点「一大一小」的呼吸效果。
        // 用 infiniteTransition 得到一个 0..2π 的连续相位，每个圆点按自身 id 偏移，
        // 再取 sin 得到 0..1 的缩放因子，让圆点错落有致地呼吸。
        val breathPhase by rememberInfiniteTransition(label = "breathPhase").animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "breathPhase",
        )

        if (collecting) {
            // 采集中：可点击的 Canvas 负责手势（轻点新增/取消、长按拖动）
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(timeoutSeconds) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val nx = down.position.x / size.width
                            val ny = down.position.y / size.height

                            // 命中检测：落点是否在某个圆点半径内
                            val hit = hitTest(nx, ny, currentPlayers)

                            // 等待长按或抬手
                            val longPress = awaitLongPressOrCancellation(down.id)
                            if (longPress != null) {
                                // 长按：命中圆点则拖动它；空位长按视为新增
                                if (hit != null) {
                                    longPress.consume()
                                    // 碰撞半径 = 最大呼吸半径（像素），保证两圆呼吸到最大也刚好相切
                                    val bodyRadiusPx = dotRadiusDp.dp.toPx() * 1.09f
                                    drag(down.id) { change ->
                                        // 目标圆心（像素）= 手指位置
                                        val targetX = change.position.x
                                        val targetY = change.position.y
                                        // 其它圆点（像素圆心）
                                        val others = currentPlayers
                                            .filter { it.id != hit.id }
                                            .map { p ->
                                                Offset(p.x * size.width, p.y * size.height)
                                            }
                                        // 碰撞 + 屏幕边界 clamp（像素级）
                                        val (px, py) = resolveCollision(
                                            targetX, targetY,
                                            others, bodyRadiusPx,
                                            size.width.toFloat(), size.height.toFloat(),
                                        )
                                        // 转归一化写回 VM
                                        onDragMove(
                                            hit.id,
                                            px / size.width,
                                            py / size.height,
                                            timeoutSeconds,
                                        )
                                        change.consume()
                                    }
                                    onDragEnd(timeoutSeconds)
                                } else {
                                    onTap(nx, ny, timeoutSeconds)
                                }
                            } else {
                                // awaitLongPressOrCancellation 返回 null = 手指已抬起 = 轻点。
                                // 无需再 waitForUpOrCancellation（up 已被上面消费掉），直接处理。
                                onTap(nx, ny, timeoutSeconds)
                            }
                        }
                    }
            ) {
                uiState.players.forEach { p ->
                    // 每个圆点按 id 错开相位，得到独立的呼吸缩放（1.0 ~ 1.18 之间）
                    val phase = breathPhase + (p.id % 8) * (Math.PI / 4).toFloat()
                    val scale = 1f + 0.09f * kotlin.math.sin(phase).toFloat()
                    drawCircle(
                        color = p.color,
                        radius = dotRadiusDp.dp.toPx() * scale,
                        center = Offset(p.x * size.width, p.y * size.height),
                    )
                }
            }
        } else {
            // 结果阶段：展示结果（背景保留圆点，结果由各工具自己画）
            resultContent()
        }

        // 顶部操作栏：返回 + 重开
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回主界面",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "重新开始",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // 底部：自定义内容（如队伍分组的队数输入）优先；否则按模式显示倒计时圈圈/确认按钮
        if (collecting) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (bottomContent != null) {
                    // 自定义底部内容（替代默认倒计时/按钮）
                    bottomContent()
                } else if (confirmationMode == ConfirmationMode.MANUAL) {
                    // 手动模式：大按钮，点它结束报名；少于 2 人时置灰（一个人无需结束）
                    Button(
                        onClick = onFinish,
                        enabled = uiState.players.size >= MIN_PLAYERS,
                    ) {
                        Text(finishLabel)
                    }
                } else {
                    // 倒计时圈圈：圆弧从满圈逐渐缩短到空
                    val ringBg = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    val ringFg = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val strokeWidth = 6.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(inset, inset)

                        // 背景圆环（淡色）
                        drawArc(
                            color = ringBg,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth),
                        )
                        // 前景圆弧：sweep 随剩余时间缩短
                        drawArc(
                            color = ringFg,
                            startAngle = -90f,
                            sweepAngle = 360f * countdownProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

/** 命中检测：返回落点 [x]/[y] 命中的最近圆点，未命中返回 null。 */
private fun hitTest(x: Float, y: Float, players: List<Player>): Player? {
    var nearest: Player? = null
    var nearestDist = Float.MAX_VALUE
    for (p in players) {
        val dx = x - p.x
        val dy = y - p.y
        val d = sqrt(dx * dx + dy * dy)
        if (d <= DOT_NORM_RADIUS && d < nearestDist) {
            nearestDist = d
            nearest = p
        }
    }
    return nearest
}

/**
 * 单边刚体碰撞（像素级）：把被拖动的目标圆心 [tx]/[ty] 从每个「其它圆点」处推开，
 * 直到与它们两两圆心距离 >= 2 × [bodyRadius]（即两圆刚好相切、绝不重叠）。
 * 其它圆点固定不动，只有目标点被推。同时 clamp 到 [screenW]×[screenH] 内（留出半径，不拖出屏）。
 *
 * @param bodyRadius 刚体半径（像素）。用最大呼吸半径锚定，保证呼吸峰值也不重叠。
 * @return 推开后的目标圆心像素坐标。
 */
private fun resolveCollision(
    tx: Float,
    ty: Float,
    others: List<Offset>,
    bodyRadius: Float,
    screenW: Float,
    screenH: Float,
): Pair<Float, Float> {
    val minDist = bodyRadius * 2f // 两圆相切的最小圆心距离 = 直径
    var cx = tx
    var cy = ty
    // 迭代几轮，处理「被多个点夹住」时依次推开后的连锁重叠
    repeat(3) {
        for (p in others) {
            val dx = cx - p.x
            val dy = cy - p.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < minDist) {
                if (d < 1e-6f) {
                    // 几乎重合：沿 x 轴向右推开
                    cx = p.x + minDist
                    cy = p.y
                } else {
                    val ratio = minDist / d
                    cx = p.x + dx * ratio
                    cy = p.y + dy * ratio
                }
            }
        }
        // 屏幕边界：留出刚体半径，圆点不拖出屏幕
        cx = cx.coerceIn(bodyRadius, screenW - bodyRadius)
        cy = cy.coerceIn(bodyRadius, screenH - bodyRadius)
    }
    return cx to cy
}
