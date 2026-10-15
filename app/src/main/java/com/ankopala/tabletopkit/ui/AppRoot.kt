package com.ankopala.tabletopkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ankopala.tabletopkit.ui.firstplayer.FirstPlayerScreen
import com.ankopala.tabletopkit.ui.firstplayer.FirstPlayerViewModel
import com.ankopala.tabletopkit.ui.home.HomeScreen
import com.ankopala.tabletopkit.ui.navigation.Screen
import com.ankopala.tabletopkit.ui.playerorder.PlayerOrderScreen
import com.ankopala.tabletopkit.ui.playerorder.PlayerOrderViewModel
import com.ankopala.tabletopkit.ui.registration.RegistrationScreen
import com.ankopala.tabletopkit.ui.settings.SettingsScreen
import com.ankopala.tabletopkit.ui.teamsplitter.TeamCountInput
import com.ankopala.tabletopkit.ui.teamsplitter.TeamSplitterScreen
import com.ankopala.tabletopkit.ui.teamsplitter.TeamSplitterViewModel
import com.ankopala.tabletopkit.vm.MainViewModel

/**
 * 根 Composable：根据当前 [Screen] 分发到对应屏。
 * 所有子屏共享同一个 [MainViewModel]（导航 + 全局设置）。
 */
@Composable
fun AppRoot(mainViewModel: MainViewModel) {
    val screen by mainViewModel.screen.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()

    when (screen) {
        Screen.Home -> HomeScreen(onNavigate = mainViewModel::navigate)

        Screen.FirstPlayer -> {
            val vm: FirstPlayerViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val countdown by vm.countdownProgress.collectAsStateWithLifecycle()
            val result by vm.result.collectAsStateWithLifecycle()
            val settled by vm.settled.collectAsStateWithLifecycle()

            vm.confirmationMode = settings.confirmationMode

            RegistrationScreen(
                uiState = uiState,
                countdownProgress = countdown,
                timeoutSeconds = settings.timeoutSeconds,
                confirmationMode = settings.confirmationMode,
                dotRadiusDp = settings.dotRadiusDp,
                finishLabel = "开始选玩家",
                onTap = vm::onTap,
                onDragMove = vm::onDragMove,
                onDragEnd = vm::onDragEnd,
                onFinish = vm::finish,
                onReset = vm::reset,
                onBack = { mainViewModel.navigate(Screen.Home) },
                resultContent = {
                    result?.let { r ->
                        FirstPlayerScreen(
                            winner = r.winner,
                            others = r.others,
                            settled = settled,
                            dotRadiusDp = settings.dotRadiusDp,
                            onSettled = vm::markSettled,
                            onBack = {},
                        )
                    }
                },
            )
        }

        Screen.PlayerOrder -> {
            val vm: PlayerOrderViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val countdown by vm.countdownProgress.collectAsStateWithLifecycle()
            val result by vm.result.collectAsStateWithLifecycle()

            vm.confirmationMode = settings.confirmationMode

            RegistrationScreen(
                uiState = uiState,
                countdownProgress = countdown,
                timeoutSeconds = settings.timeoutSeconds,
                confirmationMode = settings.confirmationMode,
                dotRadiusDp = settings.dotRadiusDp,
                finishLabel = "开始排序",
                onTap = vm::onTap,
                onDragMove = vm::onDragMove,
                onDragEnd = vm::onDragEnd,
                onFinish = vm::finish,
                onReset = vm::reset,
                onBack = { mainViewModel.navigate(Screen.Home) },
                resultContent = {
                    PlayerOrderScreen(
                        ordered = result,
                        dotRadiusDp = settings.dotRadiusDp,
                        onBack = {},
                    )
                },
            )
        }

        Screen.TeamSplitter -> {
            val vm: TeamSplitterViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val countdown by vm.countdownProgress.collectAsStateWithLifecycle()
            val isLocked by vm.isLocked.collectAsStateWithLifecycle()
            val teams by vm.teams.collectAsStateWithLifecycle()

            // 队数输入（默认 2），首次点击屏幕注册时锁定
            var teamCountInput by remember { mutableStateOf("2") }

            vm.confirmationMode = settings.confirmationMode

            RegistrationScreen(
                uiState = uiState,
                countdownProgress = countdown,
                timeoutSeconds = settings.timeoutSeconds,
                confirmationMode = settings.confirmationMode,
                dotRadiusDp = settings.dotRadiusDp,
                finishLabel = "开始分组",
                onTap = { nx, ny, timeout ->
                    // 首次点击：先锁定当前输入框的队数，再注册
                    if (!isLocked) {
                        vm.lockTeamCount(teamCountInput.toIntOrNull() ?: 2)
                    }
                    vm.onTap(nx, ny, timeout)
                },
                onDragMove = vm::onDragMove,
                onDragEnd = vm::onDragEnd,
                onFinish = vm::finish,
                onReset = vm::reset,
                onBack = { mainViewModel.navigate(Screen.Home) },
                resultContent = {
                    TeamSplitterScreen(
                        players = uiState.players,
                        teams = teams,
                        dotRadiusDp = settings.dotRadiusDp,
                    )
                },
                bottomContent = if (!isLocked) {
                    {
                        TeamCountInput(
                            value = teamCountInput,
                            onValueChange = { teamCountInput = it },
                        )
                    }
                } else {
                    null
                },
            )
        }

        Screen.Settings -> SettingsScreen(
            settings = settings,
            onSetThemeMode = mainViewModel::setThemeMode,
            onSetConfirmationMode = mainViewModel::setConfirmationMode,
            onSetTimeoutSeconds = mainViewModel::setTimeoutSeconds,
            onSetDotRadiusDp = mainViewModel::setDotRadiusDp,
            onBack = { mainViewModel.navigate(Screen.Home) },
        )
    }
}
