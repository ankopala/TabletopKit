package com.ankopala.tabletopkit.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.ui.navigation.Screen

/**
 * 工具入口的图形化小图标。每个工具用 [ToolEntry.icon] 里的几何图形表达语义。
 */
private enum class ToolIcon { FIRST_PLAYER, PLAYER_ORDER, TEAM_SPLITTER }

private data class ToolEntry(
    val screen: Screen,
    val title: String,
    val icon: ToolIcon,
)

private val tools = listOf(
    ToolEntry(Screen.FirstPlayer, "起始玩家", ToolIcon.FIRST_PLAYER),
    ToolEntry(Screen.PlayerOrder, "排序玩家", ToolIcon.PLAYER_ORDER),
    ToolEntry(Screen.TeamSplitter, "队伍分组", ToolIcon.TEAM_SPLITTER),
)

/**
 * 主界面：宫格卡片入口（三个工具 + 一个设置）。
 * 每个工具卡片 = 图形化图标 + 标题（无副标题文字）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("桌游工具箱") }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(tools) { tool ->
                Card(
                    onClick = { onNavigate(tool.screen) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ToolIconCanvas(tool.icon)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                Card(
                    onClick = { onNavigate(Screen.Settings) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SettingsIconCanvas()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 起始玩家图标：边框内四个圆，其中一个异色（代表被选中） */
@Composable
private fun ToolIconCanvas(icon: ToolIcon) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val boxSize = 72.dp

    Canvas(modifier = Modifier.size(boxSize)) {
        when (icon) {
            ToolIcon.FIRST_PLAYER -> {
                // 边框
                drawRect(
                    color = onSurface.copy(alpha = 0.4f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                val r = 9.dp.toPx()
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val d = 20.dp.toPx()
                // 四个圆：左上、右上、左下 同色，右下异色
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(cx - d, cy - d))
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(cx + d, cy - d))
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(cx - d, cy + d))
                drawCircle(color = primary, radius = r, center = Offset(cx + d, cy + d))
            }

            ToolIcon.PLAYER_ORDER -> {
                // 三个圆从左到右排成一排，带「顺序」感（中间一个略突出）
                val w = this.size.width
                val h = this.size.height
                val cy = h / 2f
                val r = 11.dp.toPx()
                val y1 = cy - 6.dp.toPx()
                val y2 = cy + 6.dp.toPx()
                drawCircle(color = onSurface.copy(alpha = 0.5f), radius = r, center = Offset(w * 0.22f, y1))
                drawCircle(color = onSurface.copy(alpha = 0.7f), radius = r, center = Offset(w * 0.50f, cy))
                drawCircle(color = primary, radius = r, center = Offset(w * 0.78f, y2))
            }

            ToolIcon.TEAM_SPLITTER -> {
                // 六个圆分成左右两堆
                val w = this.size.width
                val h = this.size.height
                val r = 10.dp.toPx()
                val leftX = w * 0.30f
                val rightX = w * 0.70f
                val topY = h * 0.30f
                val midY = h * 0.55f
                val botY = h * 0.72f
                // 左堆 3 个（同一色系）
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(leftX, topY))
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(leftX, botY))
                drawCircle(color = onSurface.copy(alpha = 0.6f), radius = r, center = Offset(leftX - 12.dp.toPx(), midY))
                // 右堆 3 个（另一色系）
                drawCircle(color = primary, radius = r, center = Offset(rightX, topY))
                drawCircle(color = primary, radius = r, center = Offset(rightX, botY))
                drawCircle(color = primary, radius = r, center = Offset(rightX + 12.dp.toPx(), midY))
            }
        }
    }
}

/** 设置图标：一个小齿轮（外圈齿 + 中间圆孔） */
@Composable
private fun SettingsIconCanvas() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val boxSize = 72.dp

    Canvas(modifier = Modifier.size(boxSize)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerR = 26.dp.toPx()      // 齿尖半径
        val bodyR = 20.dp.toPx()       // 齿根半径
        val holeR = 9.dp.toPx()        // 中间圆孔半径
        val teeth = 8

        // 齿轮主体（实心多边形近似 + 外圈齿）
        // 先画外圈 8 个齿：每个齿是一小段向外凸出的矩形，用圆周上交替 radius 的多边形近似
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until teeth * 2) {
            val angle = (Math.PI / teeth) * i - Math.PI / 2
            val r = if (i % 2 == 0) outerR else bodyR
            val x = c.x + kotlin.math.cos(angle).toFloat() * r
            val y = c.y + kotlin.math.sin(angle).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = onSurface.copy(alpha = 0.6f))

        // 中间圆孔（挖空感：用背景色画圆）
        drawCircle(
            color = surface,
            radius = holeR,
            center = c,
        )
    }
}
