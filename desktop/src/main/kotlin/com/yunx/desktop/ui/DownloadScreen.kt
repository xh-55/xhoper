/*
 * 下载页：任务列表 + 进度 + 暂停/继续/删除。
 */
package com.yunx.desktop.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yunx.desktop.core.DownloadTask
import com.yunx.desktop.core.DesktopApp
import com.yunx.desktop.core.TaskState

@Composable
fun DownloadScreen(app: DesktopApp) {
    val tasks by app.downloader.tasks.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("下载任务（${tasks.size}）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "保存到：${com.yunx.desktop.core.DesktopContext.downloadDir.absolutePath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Text("还没有下载任务。到「解析」页粘贴链接开始。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        LazyColumn {
            items(tasks, key = { it.id }) { task ->
                TaskRow(task,
                    onStart = { app.downloader.start(task.id) },
                    onPause = { app.downloader.pause(task.id) },
                    onRemove = { app.downloader.remove(task.id) }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: DownloadTask, onStart: () -> Unit, onPause: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(task.fileName, maxLines = 1)
                Row {
                    Text(
                        when (task.state) {
                            TaskState.PENDING -> "等待中"
                            TaskState.RUNNING -> "下载中"
                            TaskState.PAUSED -> "已暂停"
                            TaskState.COMPLETED -> "已完成"
                            TaskState.FAILED -> "失败：${task.error}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (task.state) {
                            TaskState.FAILED -> MaterialTheme.colorScheme.error
                            TaskState.COMPLETED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${formatSize(task.downloaded)} / ${if (task.total > 0) formatSize(task.total) else "?"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.state == TaskState.RUNNING && task.speed > 0) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${formatSize(task.speed)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            when (task.state) {
                TaskState.RUNNING -> OutlinedButton(onClick = onPause) { Text("暂停") }
                TaskState.COMPLETED -> Unit
                else -> Button(onClick = onStart) { Text("继续") }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onRemove) { Text("删除") }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { task.progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}