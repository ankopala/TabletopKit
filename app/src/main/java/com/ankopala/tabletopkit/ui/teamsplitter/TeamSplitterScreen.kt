package com.ankopala.tabletopkit.ui.teamsplitter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.domain.Team

/**
 * 队伍分组结果：按队伍重新染色 + 底部图例。
 * （分组数量已在报名前输入，这里只负责展示结果。）
 */
@Composable
fun TeamSplitterScreen(
    players: List<com.ankopala.tabletopkit.data.Player>,
    teams: List<Team>,
) {
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
                        radius = 34.dp.toPx(),
                        center = Offset(p.x * size.width, p.y * size.height),
                    )
                }
            }
        } else {
            // 已分组：按队伍重新染色
            Canvas(modifier = Modifier.fillMaxSize()) {
                teams.forEach { team ->
                    team.members.forEach { p ->
                        drawCircle(
                            color = team.color,
                            radius = 40.dp.toPx(),
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
