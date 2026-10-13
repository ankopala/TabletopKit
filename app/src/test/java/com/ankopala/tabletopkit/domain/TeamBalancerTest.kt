package com.ankopala.tabletopkit.domain

import androidx.compose.ui.graphics.Color
import com.ankopala.tabletopkit.data.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamBalancerTest {

    private fun players(n: Int) = (0 until n).map {
        Player(id = it.toLong(), x = 0f, y = 0f, color = Color(0xFF000000))
    }

    @Test
    fun `分组人数差不超过 1`() {
        val teams = balance(players(10), 3)
        assertEquals(3, teams.size)
        val sizes = teams.map { it.members.size }
        assertTrue("各队人数差应 ≤ 1", sizes.max() - sizes.min() <= 1)
    }

    @Test
    fun `所有玩家恰好被分到一队`() {
        val ps = players(10)
        val teams = balance(ps, 2)
        val total = teams.sumOf { it.members.size }
        assertEquals(ps.size, total)
    }

    @Test
    fun `teamCount 非法时报错`() {
        try {
            balance(players(5), 0)
            throw AssertionError("应抛出异常")
        } catch (e: IllegalArgumentException) {
            // 预期
        }
    }
}
