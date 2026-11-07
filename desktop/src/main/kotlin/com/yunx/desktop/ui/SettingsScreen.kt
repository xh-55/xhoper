/*
 * 设置页：Cookie 管理 + 下载目录 + 日志。
 *
 * Cookie 是桌面端最关键的配置：网盘接口要靠它才能取到直链。
 * 用户从浏览器登录网盘，按 F12 → Network 复制 Cookie 整行粘进来即可。
 */
package com.yunx.desktop.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yunx.app.data.network.SharePlatform
import com.yunx.desktop.core.DesktopApp
import com.yunx.desktop.core.DesktopContext

@Composable
fun SettingsScreen(app: DesktopApp) {
    var quarkCookie by remember { mutableStateOf(app.cookie(SharePlatform.QUARK)) }
    var saved by remember { mutableStateOf<String?>(null) }
    var logTail by remember { mutableStateOf(android.util.Log.snapshot(80)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("网盘登录", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "在浏览器登录 pan.quark.cn，按 F12 打开开发者工具 → Network → 任意请求，" +
                "把请求头里的 Cookie 整行复制下来粘到下面。\n" +
                "不填也能用：夸克分享支持游客取链（画质与速度可能受限）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = quarkCookie,
            onValueChange = { quarkCookie = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("夸克网盘 Cookie") },
            minLines = 3,
            maxLines = 8
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                app.saveCookie(SharePlatform.QUARK, quarkCookie)
                saved = "已保存"
            }) { Text("保存") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                quarkCookie = ""
                app.saveCookie(SharePlatform.QUARK, "")
                saved = "已清除"
            }) { Text("清除") }
        }
        saved?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Text("下载目录", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(DesktopContext.downloadDir.absolutePath, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(24.dp))
        Row {
            Text("运行日志", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { logTail = android.util.Log.snapshot(80) }) { Text("刷新") }
            TextButton(onClick = {
                android.util.Log.clear()
                logTail = emptyList()
            }) { Text("清空") }
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth()) {
            if (logTail.isEmpty()) {
                Text("（暂无日志）", style = MaterialTheme.typography.bodySmall)
            } else {
                logTail.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "XhOper 桌面版 · 基于 YunX（AGPL-3.0）二次开发\n" +
                "解析层直接复用 Android 工程源码，界面为桌面端重写。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}