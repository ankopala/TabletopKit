package com.ankopala.tabletopkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ankopala.tabletopkit.ui.teamsplitter.TeamCountInputScreen
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
                finishLabel = "开始排序",
                onTap = vm::onTap,
                onDragMove = vm::onDragMove,
                onDragEnd = vm::onDragEnd,
                onFinish = vm::finish,
                onReset = vm::reset,
                onBack = { mainViewModel.navigate(Screen.Home) },
                resultContent = {
                    PlayerOrderScreen(ordered = result, onBack = {})
                },
            )
        }

        Screen.TeamSplitter -> {
            val vm: TeamSplitterViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val countdown by vm.countdownProgress.collectAsStateWithLifecycle()
            val teamCount by vm.teamCount.collectAsStateWithLifecycle()
            val teams by vm.teams.collectAsStateWithLifecycle()

            if (teamCount <= 0) {
                // 报名前：先输入分成几队
                TeamCountInputScreen(
                    onConfirm = vm::setTeamCount,
                    onBack = { mainViewModel.navigate(Screen.Home) },
                )
            } else {
                vm.confirmationMode = settings.confirmationMode

                RegistrationScreen(
                    uiState = uiState,
                    countdownProgress = countdown,
                    timeoutSeconds = settings.timeoutSeconds,
                    confirmationMode = settings.confirmationMode,
                    finishLabel = "开始分组",
                    onTap = vm::onTap,
                    onDragMove = vm::onDragMove,
                    onDragEnd = vm::onDragEnd,
                    onFinish = vm::finish,
                    onReset = vm::reset,
                    onBack = { mainViewModel.navigate(Screen.Home) },
                    resultContent = {
                        TeamSplitterScreen(
                            players = uiState.players,
                            teams = teams,
                        )
                    },
                )
            }
        }

        Screen.Settings -> SettingsScreen(
            settings = settings,
            onSetThemeMode = mainViewModel::setThemeMode,
            onSetConfirmationMode = mainViewModel::setConfirmationMode,
            onSetTimeoutSeconds = mainViewModel::setTimeoutSeconds,
            onBack = { mainViewModel.navigate(Screen.Home) },
        )
    }
}
