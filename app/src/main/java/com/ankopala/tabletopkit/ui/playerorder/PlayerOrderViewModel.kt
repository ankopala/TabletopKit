package com.ankopala.tabletopkit.ui.playerorder

import com.ankopala.tabletopkit.data.Player
import com.ankopala.tabletopkit.domain.fisherYates
import com.ankopala.tabletopkit.ui.registration.RegistrationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 排序玩家：报名结束后 Fisher–Yates 洗牌，标注座位号 1..N。
 */
class PlayerOrderViewModel : RegistrationViewModel() {

    data class OrderedPlayer(
        val player: Player,
        val seat: Int,
    )

    private val _result = MutableStateFlow<List<OrderedPlayer>>(emptyList())
    val result: StateFlow<List<OrderedPlayer>> = _result.asStateFlow()

    override fun onTimeout() {
        super.onTimeout()
        val shuffled = fisherYates(uiState.value.players)
        _result.value = shuffled.mapIndexed { i, p -> OrderedPlayer(p, i + 1) }
    }

    override fun reset() {
        super.reset()
        _result.value = emptyList()
    }
}
