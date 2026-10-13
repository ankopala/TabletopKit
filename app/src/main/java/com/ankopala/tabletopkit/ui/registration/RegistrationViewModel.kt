package com.ankopala.tabletopkit.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ankopala.tabletopkit.data.ConfirmationMode
import com.ankopala.tabletopkit.data.Player
import com.ankopala.tabletopkit.domain.ColorAssigner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/** 报名阶段：采集中 / 结果展示 */
sealed interface RegistrationPhase {
    data object Collecting : RegistrationPhase
    data object Result : RegistrationPhase
}

/** 报名通用状态 */
data class RegistrationUiState(
    val phase: RegistrationPhase = RegistrationPhase.Collecting,
    val players: List<Player> = emptyList(),
)

/**
 * 圆点归一化半径（0..1 相对坐标下）。用于「轻点取消」的命中检测：
 * 落点与圆点圆心距离 <= 该值，视为「点中了这个圆点」。
 */
const val DOT_NORM_RADIUS = 0.05f

/**
 * 报名采集状态机（三个工具共用）。
 *
 * 核心机制：
 * 1. 轻点空位 → 新增圆点；轻点已有点 → 取消该点。
 * 2. 长按已有点 → 拖动（跟随手指）；长按空位 → 新增。
 * 3. 「连续 N 秒无人触碰自动停止」：每次触碰（含拖动）先取消上一个超时 Job，
 *    再新起一个协程；期间无新触碰则触发 [onTimeout]。
 * 4. [onTimeout] 是子类钩子，各自把 phase 切到 Result 并计算自己的结果。
 *
 * 倒计时进度：[countdownProgress] 是一个 1f→0f 递减的 StateFlow，
 * 每次触碰重置为 1f 并重新开始递减到 0（0 即触发超时）。UI 用它画「圆环逐渐缩短到空」。
 */
open class RegistrationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    /** 倒计时剩余比例 1f(满) → 0f(空)，供 UI 画圆环。初始 0f（空），首次触碰后才开始 */
    private val _countdownProgress = MutableStateFlow(0f)
    val countdownProgress: StateFlow<Float> = _countdownProgress.asStateFlow()

    private val colorAssigner = ColorAssigner()
    private var nextId = 0L
    private var timeoutJob: Job? = null

    /**
     * 玩家数量确定方式。由 AppRoot 根据全局设置同步进来。
     * - AUTO_COUNTDOWN：每次触碰 [restartTimeout] 启动倒计时，超时自动结束。
     * - MANUAL：不启动倒计时，等用户点 [finish] 手动结束。
     */
    var confirmationMode: ConfirmationMode = ConfirmationMode.AUTO_COUNTDOWN

    /** 纯新增：在 [x]/[y] 落一个圆点（不做避让，由上层 onTap 保证不重合）。 */
    private fun addPlayer(x: Float, y: Float, timeoutSeconds: Float) {
        if (_uiState.value.phase != RegistrationPhase.Collecting) return
        val color = colorAssigner.next(usedColors())
        val player = Player(
            id = nextId++,
            x = x,
            y = y,
            color = color,
        )
        _uiState.update { it.copy(players = it.players + player) }
        restartTimeout(timeoutSeconds)
    }

    /**
     * 轻点回调：命中已有圆点则取消，否则新增。
     */
    fun onTap(x: Float, y: Float, timeoutSeconds: Float) {
        if (_uiState.value.phase != RegistrationPhase.Collecting) return
        val hit = hitTest(x, y)
        if (hit != null) {
            removePlayer(hit.id)
        } else {
            addPlayer(x, y, timeoutSeconds)
        }
    }

    /** 长按拖动：移动 [id] 圆点到 [x]/[y]（跟随手指，暂不做碰撞）。 */
    fun onDragMove(id: Long, x: Float, y: Float, timeoutSeconds: Float) {
        if (_uiState.value.phase != RegistrationPhase.Collecting) return
        _uiState.update { state ->
            val idx = state.players.indexOfFirst { it.id == id }
            if (idx < 0) return@update state
            val updated = state.players.toMutableList()
            updated[idx] = state.players[idx].copy(
                x = x.coerceIn(0f, 1f),
                y = y.coerceIn(0f, 1f),
            )
            state.copy(players = updated)
        }
        restartTimeout(timeoutSeconds)
    }

    /** 拖动结束：重置倒计时（拖动算触碰）。 */
    fun onDragEnd(timeoutSeconds: Float) {
        if (_uiState.value.phase != RegistrationPhase.Collecting) return
        restartTimeout(timeoutSeconds)
    }

    /** 取消指定 id 的圆点。 */
    private fun removePlayer(id: Long) {
        _uiState.update { it.copy(players = it.players.filterNot { p -> p.id == id }) }
    }

    /** 命中检测：返回落点 [x]/[y] 命中的最近圆点，未命中返回 null。 */
    private fun hitTest(x: Float, y: Float): Player? {
        var nearest: Player? = null
        var nearestDist = Float.MAX_VALUE
        for (p in _uiState.value.players) {
            val dx = x - p.x
            val dy = y - p.y
            val d = sqrt(dx * dx + dy * dy)
            if (d <= DOT_NORM_RADIUS && d < nearestDist) {
                nearestDist = d
                nearest = p
            }
        }
        return nearest
    }

    private fun usedColors() = _uiState.value.players.map { it.color }.toSet()

    private fun restartTimeout(seconds: Float) {
        // 手动模式：不跑倒计时，等用户点「确认」按钮手动结束
        if (confirmationMode == ConfirmationMode.MANUAL) {
            timeoutJob?.cancel()
            timeoutJob = null
            _countdownProgress.value = 0f
            return
        }
        timeoutJob?.cancel() // 关键：每次触碰先取消旧计时
        timeoutJob = viewModelScope.launch {
            // 用真实时间戳计算进度，避免 delay 步长漂移导致「多走」时长
            val totalMs = (seconds * 1000f).toLong()
            val start = System.currentTimeMillis()
            _countdownProgress.value = 1f
            while (true) {
                val elapsed = System.currentTimeMillis() - start
                if (elapsed >= totalMs) break
                _countdownProgress.value = (1f - elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                delay(16L)
            }
            _countdownProgress.value = 0f
            onTimeout()
        }
    }

    /**
     * 手动结束采集（手动模式下由「确认」按钮触发）。
     * 直接进入结果阶段，等价于倒计时超时。
     */
    fun finish() {
        if (_uiState.value.phase != RegistrationPhase.Collecting) return
        timeoutJob?.cancel()
        timeoutJob = null
        _countdownProgress.value = 0f
        onTimeout()
    }

    /** 子类钩子：超时后进入结果阶段并计算各自结果 */
    protected open fun onTimeout() {
        _uiState.update { it.copy(phase = RegistrationPhase.Result) }
    }

    /** 重开：清空并回到采集中 */
    open fun reset() {
        timeoutJob?.cancel()
        nextId = 0L
        _countdownProgress.value = 0f
        _uiState.value = RegistrationUiState()
    }
}
