#!/usr/bin/env bash
# 云端构建脚本
# 用法:
#   ./scripts/build.sh release         构建 Release APK
#   ./scripts/build.sh debug           构建 Debug APK
#   ./scripts/build.sh clean           清理后构建 Release APK
#   ./scripts/build.sh release --sign  强制签名（即使未配置环境变量）

set -e

BUILD_TYPE=${1:-release}
SIGN=${2:-}

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}==> WiFi Battle Cloud Build${NC}"
echo -e "${YELLOW}==> Build type: $BUILD_TYPE${NC}"

if [ "$BUILD_TYPE" = "clean" ]; then
    echo -e "${YELLOW}==> Cleaning project...${NC}"
    ./gradlew clean
    BUILD_TYPE="release"
fi

# 构造 Gradle 参数
GRADLE_ARGS="assemble${BUILD_TYPE^}"
GRADLE_ARGS="$GRADLE_ARGS --no-daemon --stacktrace"

# 签名配置
if [ -n "$WFB_KEYSTORE_FILE" ] && [ -f "$WFB_KEYSTORE_FILE" ]; then
    GRADLE_ARGS="$GRADLE_ARGS \
        -PWFB_KEYSTORE_FILE=$WFB_KEYSTORE_FILE \
        -PWFB_KEYSTORE_PASSWORD=$WFB_KEYSTORE_PASSWORD \
        -PWFB_KEY_ALIAS=$WFB_KEY_ALIAS \
        -PWFB_KEY_PASSWORD=$WFB_KEY_PASSWORD"
    echo -e "${GREEN}==> Using signing config: $WFB_KEY_ALIAS${NC}"
else
    echo -e "${YELLOW}==> No keystore configured, building unsigned APK${NC}"
fi

echo -e "${GREEN}==> Running: ./gradlew $GRADLE_ARGS${NC}"
./gradlew $GRADLE_ARGS

# 收集输出
OUT_DIR="app/build/outputs/apk/$BUILD_TYPE"
if [ -d "$OUT_DIR" ]; then
    mkdir -p /dist
    cp -v $OUT_DIR/*.apk /dist/ 2>/dev/null || true
    echo -e "${GREEN}==> APKs copied to /dist/${NC}"
    ls -lh /dist/
fi

echo -e "${GREEN}==> Build complete.${NC}"
