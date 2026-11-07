#!/usr/bin/env bash
# XhOper 更新包发布脚本
#
# 用法：
#   ./publish.sh <apk 文件> <版本号> [更新说明文件]
#
# 例：
#   ./publish.sh build/app-debug.apk 1.1 notes.txt
#
# 做的事：
#   1. 把 APK 复制成 apk/XhOper-<版本号>.apk
#   2. 计算体积与 sha256
#   3. 生成 api/v1/app/latest 清单（App 的「检查更新」读的就是它）
#   4. 打印上传命令（把这几个文件传到服务器 web 根目录）

set -eu

APK="${1:-}"
VER="${2:-}"
NOTES_FILE="${3:-}"

if [ -z "$APK" ] || [ -z "$VER" ]; then
    echo "用法: $0 <apk 文件> <版本号> [更新说明文件]"
    exit 1
fi

[ -f "$APK" ] || { echo "找不到 APK: $APK"; exit 1; }

HOST="43.128.136.224"
ROOT="$(cd "$(dirname "$0")" && pwd)"
NAME="XhOper-$VER.apk"

mkdir -p "$ROOT/apk" "$ROOT/api/v1/app"

cp -f "$APK" "$ROOT/apk/$NAME"
SIZE=$(wc -c < "$ROOT/apk/$NAME" | tr -d ' ')
SHA=$(sha256sum "$ROOT/apk/$NAME" | cut -d' ' -f1)

if [ -n "$NOTES_FILE" ] && [ -f "$NOTES_FILE" ]; then
    NOTES=$(python3 -c 'import json,sys; print(json.dumps(open(sys.argv[1],encoding="utf-8").read()))' "$NOTES_FILE")
else
    NOTES='"更新内容请在发布时填写。"'
fi

# versionCode 用分段数字拼出来，保证单调递增（x.y.z → x*10000+y*100+z）
VC=$(python3 -c 'v=sys.argv[1].split("."); print(sum(int(v[i] if i < len(v) else 0) * (100 ** (2 - i)) for i in range(3)))' "$VER")

cat > "$ROOT/api/v1/app/latest" <<EOF
{
  "code": 0,
  "message": "ok",
  "data": {
    "versionName": "$VER",
    "versionCode": $VC,
    "notes": $NOTES,
    "apkName": "$NAME",
    "apkUrl": "http://$HOST/apk/$NAME",
    "size": $SIZE,
    "sha256": "$SHA",
    "publishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "htmlUrl": ""
  }
}
EOF

echo "已生成清单："
cat "$ROOT/api/v1/app/latest"
echo
echo "APK: $ROOT/apk/$NAME（$SIZE 字节，sha256 $SHA）"
echo
echo "上传（任选其一）："
echo "  sshpass -p '<密码>' scp -r \"$ROOT/api\" \"$ROOT/apk\" root@$HOST:/var/www/xhoper/"
echo "  rsync -avz --progress \"$ROOT/\" root@$HOST:/var/www/xhoper/"