package com.ankopala.tabletopkit.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ankopala.tabletopkit.data.AppSettings
import com.ankopala.tabletopkit.data.ConfirmationMode
import com.ankopala.tabletopkit.data.MAX_DOT_RADIUS_DP
import com.ankopala.tabletopkit.data.MIN_DOT_RADIUS_DP
import com.ankopala.tabletopkit.data.ThemeMode

/**
 * 设置页：主题模式三档 + 玩家数量确定方式 + 超时秒数 + 色板偏好（占位）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetConfirmationMode: (ConfirmationMode) -> Unit,
    onSetTimeoutSeconds: (Float) -> Unit,
    onSetDotRadiusDp: (Float) -> Unit,
    onBack: () -> Unit,
) {
    var timeoutText by remember(settings.timeoutSeconds) {
        mutableStateOf(settings.timeoutSeconds.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("主题模式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("玩家数量确定方式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ConfirmationMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.confirmationMode == mode,
                        onClick = { onSetConfirmationMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ConfirmationMode.entries.size,
                        ),
                    ) {
                        Text(
                            when (mode) {
                                ConfirmationMode.AUTO_COUNTDOWN -> "自动倒计时"
                                ConfirmationMode.MANUAL -> "手动点击"
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("超时秒数（连续无触碰自动停止报名）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = timeoutText,
                    onValueChange = { new ->
                        // 只允许数字和小数点
                        val filtered = new.filter { it.isDigit() || it == '.' }
                        timeoutText = filtered
                        filtered.toFloatOrNull()?.let {
                            onSetTimeoutSeconds(it)
                        }
                    },
                    label = { Text("秒") },
                    singleLine = true,
                    enabled = settings.confirmationMode == ConfirmationMode.AUTO_COUNTDOWN,
                    modifier = Modifier.width(120.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (settings.confirmationMode == ConfirmationMode.AUTO_COUNTDOWN) {
                        "默认 2 秒，最小 0.5 秒"
                    } else {
                        "手动模式下无需倒计时"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("圆点大小（${settings.dotRadiusDp.toInt()} dp）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Slider(
                value = settings.dotRadiusDp,
                onValueChange = onSetDotRadiusDp,
                valueRange = MIN_DOT_RADIUS_DP..MAX_DOT_RADIUS_DP,
            )
            Text(
                text = "范围 ${MIN_DOT_RADIUS_DP.toInt()} ~ ${MAX_DOT_RADIUS_DP.toInt()} dp，默认 48 dp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
