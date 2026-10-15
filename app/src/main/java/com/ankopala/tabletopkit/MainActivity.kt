package com.ankopala.tabletopkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ankopala.tabletopkit.data.ThemeMode
import com.ankopala.tabletopkit.ui.AppRoot
import com.ankopala.tabletopkit.ui.theme.TabletopKitTheme
import com.ankopala.tabletopkit.vm.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val themeMode: ThemeMode = settings.themeMode

            TabletopKitTheme(themeMode = themeMode) {
                AppRoot(mainViewModel)
            }
        }
    }
}