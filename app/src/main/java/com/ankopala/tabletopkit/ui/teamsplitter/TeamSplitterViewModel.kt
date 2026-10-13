package com.ankopala.tabletopkit.ui.teamsplitter

import com.ankopala.tabletopkit.domain.Team
import com.ankopala.tabletopkit.domain.balance
import com.ankopala.tabletopkit.ui.registration.RegistrationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 队伍分组：报名前先输入「分成 N 队」，报名结束后自动按 N 均分。
 *
 * 流程：
 * 1. 进入工具后先设置 [teamCount]（N 队）。
 * 2. 报名采集。
 * 3. 超时后 [onTimeout] 自动调用 [split]，按已设置的 N 分组展示。
 */
class TeamSplitterViewModel : RegistrationViewModel() {

    /** 已设置的分组数量，<= 0 表示尚未设置 */
    private val _teamCount = MutableStateFlow(0)
    val teamCount: StateFlow<Int> = _teamCount.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    fun setTeamCount(count: Int) {
        _teamCount.value = count
    }

    /** 报名结束后自动按已设置的 N 分组 */
    override fun onTimeout() {
        super.onTimeout()
        split(_teamCount.value)
    }

    fun split(teamCount: Int) {
        if (teamCount <= 0) return
        _teams.value = balance(uiState.value.players, teamCount)
    }

    override fun reset() {
        super.reset()
        _teamCount.value = 0
        _teams.value = emptyList()
    }
}
