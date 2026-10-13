package com.ankopala.tabletopkit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShufflerTest {

    @Test
    fun `洗牌后元素集合不变`() {
        val list = listOf(1, 2, 3, 4, 5, 6, 7, 8)
        val shuffled = fisherYates(list)
        assertEquals(list.size, shuffled.size)
        assertEquals(list.sorted(), shuffled.sorted())
    }

    @Test
    fun `洗牌不修改原列表`() {
        val list = listOf(1, 2, 3, 4, 5)
        val before = list.toList()
        fisherYates(list)
        assertEquals(before, list)
    }

    @Test
    fun `空列表与单元素列表安全`() {
        assertEquals(emptyList<Int>(), fisherYates<Int>(emptyList()))
        assertEquals(listOf(1), fisherYates(listOf(1)))
    }
}
