package com.ankopala.tabletopkit.ui.teamsplitter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 队伍数量输入区块：显示「分成几队？」标题 + 队伍数量输入框。
 * 作为报名页底部的常驻 UI（无需「开始报名」按钮，点击屏幕即报名）。
 */
@Composable
fun TeamCountInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "分成几队？",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                onValueChange(new.filter { it.isDigit() })
            },
            label = { Text("队伍数量") },
            singleLine = true,
            modifier = Modifier.width(160.dp),
        )
    }
}
