package com.ankopala.tabletopkit

import android.app.Application
import com.ankopala.tabletopkit.data.SettingsRepository

/**
 * 自定义 Application：持有全局单例 [SettingsRepository]，
 * 供 MainViewModel（AndroidViewModel）在构造时取用。
 */
class TabletopKitApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
