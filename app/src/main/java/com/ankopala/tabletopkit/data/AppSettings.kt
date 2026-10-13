package com.ankopala.tabletopkit.data

/**
 * 「玩家数量确定方式」：报名阶段如何结束、进入结果。
 */
enum class ConfirmationMode {
    /** 自动倒计时：连续 N 秒无人触碰后自动停止采集 */
    AUTO_COUNTDOWN,

    /** 手动点击：不跑倒计时，由某人点「确认」按钮结束采集 */
    MANUAL,
}

/**
 * App 的全局设置集合，最终通过 DataStore Preferences 持久化。
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 连续 N 秒无人触碰屏幕后自动停止采集，默认 2 秒，下限 0.5 秒（仅 AUTO_COUNTDOWN 模式生效） */
    val timeoutSeconds: Float = 2f,
    /** 玩家数量确定方式，默认自动倒计时 */
    val confirmationMode: ConfirmationMode = ConfirmationMode.AUTO_COUNTDOWN,
    /** 色板偏好（预留扩展，当前仅 "default" 默认色板） */
    val paletteId: String = "default",
)
