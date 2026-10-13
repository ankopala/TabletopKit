package com.ankopala.tabletopkit.domain

/**
 * Fisher–Yates 洗牌算法：把列表随机打乱，返回一个新列表（不修改原列表）。
 *
 * 思路：从末尾往前，每次在当前 [0..i] 范围内随机取一个下标 j，
 * 交换 i 与 j 位置的元素。保证每种排列等概率。
 */
fun <T> fisherYates(list: List<T>): List<T> {
    val arr = list.toMutableList()
    for (i in arr.size - 1 downTo 1) {
        val j = (0..i).random()
        val tmp = arr[i]
        arr[i] = arr[j]
        arr[j] = tmp
    }
    return arr
}
