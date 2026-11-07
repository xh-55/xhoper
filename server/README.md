# XhOper 自建更新服务

App 侧已把原版 YunX 的 GitHub 云更新通道整条移除，改为请求本服务器。

## 已部署状态（2026-11-06 实测通过）

| 项目 | 值 |
| --- | --- |
| 主机 | 43.128.136.224（OpenCloudOS 9.6 + 宝塔面板 + nginx） |
| 站点根目录 | `/www/wwwroot/panel_ssl_site` |
| 站点配置 | `/www/server/panel/vhost/nginx/43.128.136.224.conf` |
| 扩展配置 | `/www/server/panel/vhost/nginx/extension/43.128.136.224/xhoper.conf` |

已生效的接口：

| 接口 | 状态 |
| --- | --- |
| `GET /api/v1/app/latest` | 200，`application/json` |
| `GET /api/v1/announcements` | 200，`application/json`，空列表 |
| `GET /apk/XhOper-1.0.apk` | 200，`application/vnd.android.package-archive`，支持 Range（206） |
| 不存在的路径 | 404 |

## 一、目录结构

```
/www/wwwroot/panel_ssl_site/
├── api/v1/app/latest      # 更新清单（无扩展名，按 JSON 返回）
├── api/v1/announcements   # 公告列表（空列表）
└── apk/XhOper-<版本>.apk  # 更新包
```

对应代码：
- 更新：`app/src/main/kotlin/com/yunx/app/data/update/AppUpdateServer.kt`（`BASE_URL` 是唯一常量）
- 公告：`app/src/main/kotlin/com/yunx/app/data/announcement/AnnouncementApi.kt`

## 二、关键配置（为什么需要它）

宝塔默认对无扩展名文件返回 `application/octet-stream`，App 解析会失败，所以必须用 `default_type` 覆盖。
配置放在**宝塔扩展目录**而不是站点主配置里，这样在面板上改站点设置（保存/切伪静态/申请证书）都不会把它冲掉：

```
/www/server/panel/vhost/nginx/extension/43.128.136.224/xhoper.conf
```

内容见本仓库 `server/nginx-xhoper.conf`（`location ^~ /api/` 与 `location ^~ /apk/`）。

改完执行：

```
nginx -t && nginx -s reload
```

## 三、发布新版本

```
./publish.sh ../app/build/outputs/apk/debug/app-debug.apk 1.1 notes.txt
```

脚本会复制 APK、算 sha256、生成清单、打印上传命令。上传时**务必顺手改权限**，否则 nginx 以 www 用户读不到会返回 403：

```
scp apk/XhOper-1.1.apk root@43.128.136.224:/www/wwwroot/panel_ssl_site/apk/
ssh root@43.128.136.224 'chown www:www /www/wwwroot/panel_ssl_site/apk/*.apk && chmod 644 /www/wwwroot/panel_ssl_site/apk/*.apk'
```

`scp` 上去的文件默认是 `-rw------- root root`，这一步不能省。

## 四、更新清单字段

App 端解析逻辑见 `AppUpdateServer.requestLatest`：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `versionName` | 是 | 版本号，如 `1.1`；比已装版本高才会弹更新 |
| `versionCode` | 否 | 仅记录用，App 比较的是 versionName |
| `notes` | 否 | 更新说明，纯文本，弹窗直接显示 |
| `apkName` | 否 | 保存到手机的文件名，缺省 `XhOper-<版本>.apk` |
| `apkUrl` | 是 | APK 直链，App 用内置下载器下载 |
| `size` | 否 | 字节数，给了弹窗会显示体积 |
| `sha256` | 否 | 校验值，当前版本未强制校验 |
| `publishedAt` | 否 | 发布时间（ISO8601） |

没有新版本时，让接口返回 **HTTP 404** 即可（把 `api/v1/app/latest` 删掉或改名），
App 会显示「服务器暂无更新」并当作无更新处理。

## 五、注意事项

1. **明文 HTTP**：`network_security_config.xml` 为 `43.128.136.224` 单独放行了明文。
   上 HTTPS 后请把 `AppUpdateServer.BASE_URL` 与 `AnnouncementApi.BASE_URL` 改成 `https://`，
   并删掉那条 `domain-config`。

2. **域名替代 IP**：换域名只改两个 `BASE_URL` 和 nginx `server_name`，其它代码不用动。

3. **签名一致**：更新包必须与已安装版本签名相同，否则系统拒绝覆盖安装。
   debug 包用 `debug.keystore`；正式发布请换 release 签名。

4. **不要动 `panel_ssl_site` 里的既有文件**：该站点原本只有 404.html 和 index.html，
   本次只新增了 `api/` 与 `apk/` 两个目录。
## 六、液态玻璃导航栏：已放弃（记录以免重复尝试）

需求是把 `/storage/emulated/0/KernelSU-main/` 的悬浮液态玻璃导航栏移植过来。
源码可以拷（`ui/component/FloatingBottomBar.kt` + `liquid/{Lens,InnerShadow,Vibrancy,CombinedBackdrop}.kt`，
共 944 行），但它的玻璃质感依赖 `top.yukonga.miuix.kmp:miuix-blur-android`，实测有硬性连带升级：

| 要求 | miuix-blur 0.9.4 | 本工程当时 |
| --- | --- | --- |
| compileSdk | 37 | 36 |
| minSdk | 33 | 24 |
| AGP | 9.1.0 起 | 8.13.0 |
| Gradle | 9.3.1 起 | 9.1.0 |
| Compose | 1.12.0（连同 animation/foundation/ui 全量上浮） | BOM 2026.06（约 1.9） |

即：为了一个导航栏，要把 compileSdk → 37、minSdk → 33（放弃 Android 12 及以下）、
AGP → 9.1.0、Gradle → 9.3.1（需重新下载 ~130 MB 分发包）、Compose 全线上浮到 1.12。
任一步不满足，`checkDebugAarMetadata` 会直接失败。

结论：**2026-11-06 决定暂时放弃，导航栏保持原生的 Expressive ShortNavigationBar。**
以后要做，两条路：

1. **整体升级工具链**（按上表逐项升级，风险最高的是 Compose 1.9 → 1.12 对现有 285 个文件的影响）；
2. **用原生 Compose 自己实现等效视觉**（半透明渐变 + 高光描边 + blur 修饰符 + 弹簧动画胶囊），
   不引入 miuix，minSdk 可保持 24 —— 效果接近但不是 iOS 那种真折射。
