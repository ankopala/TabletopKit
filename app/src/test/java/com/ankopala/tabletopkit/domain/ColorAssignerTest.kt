package com.ankopala.tabletopkit.domain

import androidx.compose.ui.graphics.Color
import com.ankopala.tabletopkit.core.PLAYER_PALETTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorAssignerTest {

    @Test
    fun `前 N 个颜色互不重复`() {
        val assigner = ColorAssigner()
        val used = mutableSetOf<Color>()
        repeat(PLAYER_PALETTE.size) {
            val c = assigner.next(used)
            assertTrue("第 ${it + 1} 个颜色不应重复", used.add(c))
        }
    }

    @Test
    fun `色板用尽后复用`() {
        val assigner = ColorAssigner()
        val used = PLAYER_PALETTE.toSet()
        val c = assigner.next(used)
        assertTrue("用尽后应从色板内取色", c in PLAYER_PALETTE)
    }
}
