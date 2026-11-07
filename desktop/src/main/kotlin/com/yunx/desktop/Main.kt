/*
 * XhOper Desktop 主入口。
 *
 * 界面结构与 Android 端保持一致：
 *   解析（粘贴链接 → 浏览文件 → 下载） / 下载（任务列表） / 设置 / 日志
 *
 * 业务逻辑全部复用 Android 工程 data/network 下的实现，
 * 这里只负责把结果画出来、把用户操作转成调用。
 */
package com.yunx.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yunx.desktop.core.DesktopApp
import com.yunx.desktop.ui.DownloadScreen
import com.yunx.desktop.ui.ResolveScreen
import com.yunx.desktop.ui.SettingsScreen
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "XhOper · 云析"
    ) {
        MaterialTheme {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val app = remember { DesktopApp() }
    var tab by remember { mutableStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // 顶部导航
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XhOper", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(16.dp))
                listOf("解析", "下载", "设置").forEachIndexed { i, label ->
                    if (tab == i) {
                        Button(onClick = { tab = i }) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { tab = i }) { Text(label) }
                    }
                }
            }
            Divider()

            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> ResolveScreen(app)
                    1 -> DownloadScreen(app)
                    2 -> SettingsScreen(app)
                }
            }
        }
    }
}