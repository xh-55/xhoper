/*
 * 解析页：粘贴链接 → 建立会话 → 浏览目录 → 点击下载。
 */
package com.yunx.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yunx.desktop.core.DesktopApp
import com.yunx.desktop.core.RemoteEntry
import kotlinx.coroutines.launch

@Composable
fun ResolveScreen(app: DesktopApp) {
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("粘贴分享链接", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例：https://pan.quark.cn/s/xxxxxx 提取码 abcd") },
            minLines = 2,
            maxLines = 4
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it },
                modifier = Modifier.width(160.dp),
                placeholder = { Text("提取码（可留空）") },
                singleLine = true
            )
            Spacer(Modifier.width(12.dp))
            Button(
                enabled = !busy && link.isNotBlank(),
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        entries = emptyList()
                        val r = app.resolveLink(link, pwd.takeIf { it.isNotBlank() })
                        r.onSuccess { s ->
                            message = "已连接：${s.title}"
                            app.listFiles().onSuccess { entries = it }
                                .onFailure { message = "列目录失败：${it.message}" }
                        }.onFailure { message = "解析失败：${it.message}" }
                        busy = false
                    }
                }
            ) { Text("解析") }

            if (busy) {
                Spacer(Modifier.width(12.dp))
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = if (it.contains("失败") || it.contains("不支持")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }

        if (entries.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("共 ${entries.size} 项", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        app.listFiles("0").onSuccess { entries = it }
                        busy = false
                    }
                }) { Text("回到根目录") }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    FileRow(entry) {
                        if (entry.file.isdir) {
                            scope.launch {
                                busy = true
                                app.listFiles(entry.file.fid)
                                    .onSuccess { entries = it; message = "进入 ${entry.file.fname}" }
                                    .onFailure { message = "打开失败：${it.message}" }
                                busy = false
                            }
                        } else {
                            scope.launch {
                                busy = true
                                app.enqueueDownload(entry)
                                    .onSuccess { message = "已加入下载：${it.name}" }
                                    .onFailure { message = "取直链失败：${it.message}" }
                                busy = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(entry: RemoteEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (entry.file.isdir) "📁" else "📄")
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.file.fname, maxLines = 1)
            if (!entry.file.isdir && entry.file.fsize > 0) {
                Text(
                    formatSize(entry.file.fsize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            if (entry.file.isdir) "打开" else "下载",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format("%.2f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format("%.1f MB", bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> String.format("%.1f KB", bytes / (1L shl 10).toDouble())
    else -> "$bytes B"
}