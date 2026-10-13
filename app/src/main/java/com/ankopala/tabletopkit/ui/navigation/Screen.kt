package com.ankopala.tabletopkit.ui.navigation

/**
 * 全 App 的顶层路由。用 sealed interface 表达「当前该显示哪个屏」。
 *
 * 因为工具只有 3 个 + 1 个设置，数量很少，用 when(screen) 直接分发即可，
 * 不引入 navigation-compose（保持最小依赖）。
 */
sealed interface Screen {
    data object Home : Screen
    data object FirstPlayer : Screen
    data object PlayerOrder : Screen
    data object TeamSplitter : Screen
    data object Settings : Screen
}
