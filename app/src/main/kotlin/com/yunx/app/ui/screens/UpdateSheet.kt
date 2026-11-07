/*
 * XhOper
 * Copyright (C) 2026 任我行
 *
 * 本文件是「云析 · YunX」(Copyright (C) 2026 CYQawa, AGPL-3.0) 衍生作品的一部分。
 * 该程序是自由软件：你可以依据 GNU Affero 通用公共许可证（AGPL-3.0）的条款
 * 重新发布和/或修改它。完整许可证见仓库根目录的 LICENSE 文件。
 */

package com.yunx.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yunx.app.data.update.UpdateChecker

/**
 * 发现新版本的底部弹窗（XhOper 自建更新通道）。
 *
 * 与原版 YunX 的差别：
 * - 所有 GitHub 入口（直连 / 镜像站 / 打开 Release 页面）已移除；
 * - 不再从 Release 说明里提取「网盘下载」条目做网盘更新；
 * - 只剩一个主按钮「立即下载更新」，走**应用内下载器**把 APK 下到手机的 Download 目录，
 *   下载完成后在「下载」页点「打开」即可安装。
 *
 * ★ 弹窗只有这一处实现：启动检查、设置页手动检查、设置页开发调试预览全部复用它，
 *   状态与下载逻辑由 MainScreen 统一持有。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    currentVersion: String,
    release: UpdateChecker.Release,
    /** 应用内下载更新包（内置下载器落盘到 Download 目录，并切到下载页） */
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // APK 体积：服务端未给 size 时返回 0，此时不显示体积
    val apkSize = release.assets.firstOrNull { it.name.endsWith(".apk", true) }?.size ?: 0L

    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            // 标题行：图标 + 标题 + 版本号
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "发现新版本",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "当前 $currentVersion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (apkSize > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatApkSize(apkSize),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "更新内容",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            // 更新说明：纯文本显示（可滚动 + 限高，长说明不撑爆弹窗）
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = release.body.ifBlank { "暂无更新说明" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 唯一的主按钮：应用内下载（不再跳浏览器 / GitHub）
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("立即下载更新")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 弱按钮：忽略本次 / 稍后
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onIgnore) {
                    Text("忽略本次", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onLater) {
                    Text("稍后", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 字节数转人类可读体积（只用于弹窗上的体积提示） */
private fun formatApkSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1) String.format("%.1f MB", mb) else "${bytes / 1024} KB"
}