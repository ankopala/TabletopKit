package com.ankopala.tabletopkit.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 文件级单例 DataStore（一个进程只能有一个同名实例）
private val Context.dataStore by preferencesDataStore(name = "settings")

/** 倒计时秒数下限 */
const val MIN_TIMEOUT_SECONDS = 0.5f

/** 圆点半径允许范围（dp） */
const val MIN_DOT_RADIUS_DP = 40f
const val MAX_DOT_RADIUS_DP = 56f

/**
 * 设置仓库：把 [AppSettings] 与 DataStore Preferences 之间做读写映射。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        // 注意：v2 后缀。旧版此 key 存的是 Int，改成 Float 后若沿用同名 key，
        // DataStore 会因类型不匹配抛 ClassCastException 导致闪退，故换新 key 名。
        val TIMEOUT_SECONDS = floatPreferencesKey("timeout_seconds_v2")
        val CONFIRMATION_MODE = stringPreferencesKey("confirmation_mode")
        val DOT_RADIUS_DP = floatPreferencesKey("dot_radius_dp")
        val PALETTE_ID = stringPreferencesKey("palette_id")
    }

    /** 读取设置，任何变更都会推送新的 AppSettings */
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            timeoutSeconds = (runCatching { prefs[Keys.TIMEOUT_SECONDS] }.getOrNull() ?: 2f)
                .coerceAtLeast(MIN_TIMEOUT_SECONDS),
            confirmationMode = runCatching {
                ConfirmationMode.valueOf(prefs[Keys.CONFIRMATION_MODE] ?: "AUTO_COUNTDOWN")
            }.getOrDefault(ConfirmationMode.AUTO_COUNTDOWN),
            dotRadiusDp = (runCatching { prefs[Keys.DOT_RADIUS_DP] }.getOrNull() ?: 48f)
                .coerceIn(MIN_DOT_RADIUS_DP, MAX_DOT_RADIUS_DP),
            paletteId = prefs[Keys.PALETTE_ID] ?: "default",
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setTimeoutSeconds(seconds: Float) {
        context.dataStore.edit {
            it[Keys.TIMEOUT_SECONDS] = seconds.coerceAtLeast(MIN_TIMEOUT_SECONDS)
        }
    }

    suspend fun setConfirmationMode(mode: ConfirmationMode) {
        context.dataStore.edit { it[Keys.CONFIRMATION_MODE] = mode.name }
    }

    suspend fun setDotRadiusDp(dp: Float) {
        context.dataStore.edit {
            it[Keys.DOT_RADIUS_DP] = dp.coerceIn(MIN_DOT_RADIUS_DP, MAX_DOT_RADIUS_DP)
        }
    }

    suspend fun setPaletteId(id: String) {
        context.dataStore.edit { it[Keys.PALETTE_ID] = id }
    }
}
