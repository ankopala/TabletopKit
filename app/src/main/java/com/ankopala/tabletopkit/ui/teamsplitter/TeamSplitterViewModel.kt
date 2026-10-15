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
 * 1. 进入工具后，底部显示「分成几队 + 队伍数量」输入框（默认 2 队），无需「开始报名」按钮。
 * 2. 玩家第一次点击屏幕注册时，[lockTeamCount] 锁定当前输入框的队数（此后不可改）。
 * 3. 报名采集继续（点击注册 / 超时自动结束）。
 * 4. 超时后 [onTimeout] 自动调用 [split]，按已锁定的 N 分组展示。
 */
class TeamSplitterViewModel : RegistrationViewModel() {

    /** 已设置的分组数量，<= 0 表示尚未设置 */
    private val _teamCount = MutableStateFlow(0)
    val teamCount: StateFlow<Int> = _teamCount.asStateFlow()

    /** 队数是否已锁定（首次点击后锁定，此后不可改） */
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    /** 锁定队数（首次点击屏幕时由 AppRoot 调用） */
    fun lockTeamCount(count: Int) {
        if (_isLocked.value) return
        _teamCount.value = count.coerceAtLeast(1)
        _isLocked.value = true
    }

    /** 报名结束后自动按已锁定的 N 分组 */
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
        _isLocked.value = false
        _teams.value = emptyList()
    }
}
