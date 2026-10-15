package com.ankopala.tabletopkit.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankopala.tabletopkit.TabletopKitApp
import com.ankopala.tabletopkit.data.AppSettings
import com.ankopala.tabletopkit.data.ConfirmationMode
import com.ankopala.tabletopkit.data.ThemeMode
import com.ankopala.tabletopkit.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 根 ViewModel：负责「顶层导航」+「全局设置」。
 *
 * - [screen] 决定当前显示哪个屏（Home / 三个工具 / Settings）。
 * - [settings] 从 DataStore 读取，主题/超时/色板都从这里取，且会被持久化。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TabletopKitApp).settingsRepository

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun navigate(screen: Screen) {
        _screen.value = screen
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }

    fun setTimeoutSeconds(seconds: Float) = viewModelScope.launch { repo.setTimeoutSeconds(seconds) }

    fun setConfirmationMode(mode: ConfirmationMode) = viewModelScope.launch { repo.setConfirmationMode(mode) }

    fun setDotRadiusDp(dp: Float) = viewModelScope.launch { repo.setDotRadiusDp(dp) }

    fun setPaletteId(id: String) = viewModelScope.launch { repo.setPaletteId(id) }
}
