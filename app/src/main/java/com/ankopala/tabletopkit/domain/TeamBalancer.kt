package com.ankopala.tabletopkit.domain

import androidx.compose.ui.graphics.Color
import com.ankopala.tabletopkit.core.PLAYER_PALETTE
import com.ankopala.tabletopkit.data.Player

/**
 * 一个队伍：序号 + 成员 + 该队展示颜色。
 */
data class Team(
    val index: Int,
    val members: List<Player>,
    val color: Color,
)

/**
 * 队伍分组：把玩家随机、尽量均分到 N 队。
 *
 * 做法：先 Fisher–Yates 洗牌，再按「轮流发牌」方式分配
 * （第 i 个玩家进 i % teamCount 队），天然保证各队人数差 ≤ 1。
 */
fun balance(players: List<Player>, teamCount: Int): List<Team> {
    require(teamCount > 0) { "teamCount 必须大于 0" }
    val shuffled = fisherYates(players)
    val buckets = List(teamCount) { mutableListOf<Player>() }
    shuffled.forEachIndexed { i, p -> buckets[i % teamCount].add(p) }
    return buckets.mapIndexed { i, members ->
        Team(
            index = i,
            members = members,
            color = PLAYER_PALETTE[i % PLAYER_PALETTE.size],
        )
    }
}
