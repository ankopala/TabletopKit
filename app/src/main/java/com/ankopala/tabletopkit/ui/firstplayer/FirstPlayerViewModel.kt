package com.ankopala.tabletopkit.ui.firstplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ankopala.tabletopkit.data.Player
import com.ankopala.tabletopkit.ui.registration.RegistrationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 起始玩家：报名结束后随机选 1 个点作为胜者。
 */
class FirstPlayerViewModel : RegistrationViewModel() {

    data class FirstPlayerResult(
        val winner: Player,
        val others: List<Player>,
    )

    private val _result = MutableStateFlow<FirstPlayerResult?>(null)
    val result: StateFlow<FirstPlayerResult?> = _result.asStateFlow()

    /**
     * 结果动画是否已播放完成。true 表示「当前结果」的扩散/回收动画已经演过，
     * 重新进入界面时应直接显示最终态（全屏填充），不再重播动画。
     */
    private val _settled = MutableStateFlow(false)
    val settled: StateFlow<Boolean> = _settled.asStateFlow()

    override fun onTimeout() {
        super.onTimeout()
        val players = uiState.value.players
        if (players.isEmpty()) {
            reset() // 无人报名：回到采集
            return
        }
        val winner = players.random()
        _result.value = FirstPlayerResult(winner, players.filter { it.id != winner.id })
        _settled.value = false // 新结果：需要播放动画
    }

    /** 由 UI 在结果动画播放完成后调用 */
    fun markSettled() {
        _settled.value = true
    }

    override fun reset() {
        super.reset()
        _result.value = null
        _settled.value = false
    }
}
