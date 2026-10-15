package com.ankopala.tabletopkit.ui.teamsplitter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.domain.Team

/**
 * 队伍分组结果：按队伍重新染色 + 底部图例。
 * （分组数量已在报名前输入，这里只负责展示结果。）
 *
 * 动画：每个圆点先「放大再缩小回原大小」（缩放脉冲，分组结果出现时触发一次），
 * 之后保持持续的呼吸效果（infiniteTransition 驱动）。
 */
@Composable
fun TeamSplitterScreen(
    players: List<com.ankopala.tabletopkit.data.Player>,
    teams: List<Team>,
    dotRadiusDp: Float,
) {
    // 一次性缩放脉冲：0.7 → 1.25 → 1.0（放大再缩回原大小）
    val pulse = remember { Animatable(0.7f) }
    LaunchedEffect(teams) {
        pulse.snapTo(0.7f)
        // 放大阶段放慢（spring 本身较慢，这里用更柔和阻尼）
        pulse.animateTo(
            1.25f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
        // 缩回阶段也放慢
        pulse.animateTo(1.0f, tween(durationMillis = 600, easing = LinearEasing))
    }

    // 持续呼吸：1.0 ↔ 1.12 无限循环
    val breathScale by rememberInfiniteTransition(label = "teamBreath").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "teamBreathScale",
    )

    // 最终缩放 = 脉冲 × 呼吸
    val scale = pulse.value * breathScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (teams.isEmpty()) {
            // 理论上不会走到这里（超时后会自动分组），保留兜底：显示原始彩色圆点
            Canvas(modifier = Modifier.fillMaxSize()) {
                players.forEach { p ->
                    drawCircle(
                        color = p.color,
                        radius = dotRadiusDp.dp.toPx() * scale,
                        center = Offset(p.x * size.width, p.y * size.height),
                    )
                }
            }
        } else {
            // 已分组：按队伍重新染色，圆点带「脉冲 × 呼吸」缩放
            Canvas(modifier = Modifier.fillMaxSize()) {
                teams.forEach { team ->
                    team.members.forEach { p ->
                        drawCircle(
                            color = team.color,
                            radius = dotRadiusDp.dp.toPx() * scale,
                            center = Offset(p.x * size.width, p.y * size.height),
                        )
                    }
                }
            }
        }

        // 底部图例：每队一个色块 + 标签
        if (teams.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                teams.forEach { team ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(team.color, CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "第${team.index + 1}队(${team.members.size}人)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
