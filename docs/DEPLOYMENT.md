# 云端部署与构建

## 1. 本地构建

### 1.1 前置条件

- JDK 17
- Android SDK 34
- Gradle 8.7（由 `gradlew` 自动下载）

### 1.2 命令

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# 清理
./gradlew clean
```

APK 产物位置：

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

## 2. Docker 构建

### 2.1 一键构建

```bash
docker-compose up --build
```

### 2.2 自定义签名

设置环境变量：

```bash
export WFB_KEYSTORE_FILE=/path/to/release.keystore
export WFB_KEYSTORE_PASSWORD=yourpassword
export WFB_KEY_ALIAS=youralias
export WFB_KEY_PASSWORD=yourkeypassword
docker-compose up --build
```

APK 将输出到 `./dist/` 目录。

### 2.3 直接使用 Docker

```bash
docker build -t wifibattle/builder .
docker run --rm -v $(pwd)/dist:/dist wifibattle/builder release
```

## 3. GitHub Actions 云端构建

详细的 GitHub Actions 配置与 CI/CD 流程参见 [CI-CD.md](CI-CD.md)。

### 3.1 快速开始

```bash
git tag v1.0.0
git push origin v1.0.0   # 自动触发构建并发布 Release
```

### 3.2 配置签名

详细的 Secret 配置参见 [CI-CD.md § 1](../.github/workflows/SECRETS.md)。

### 3.3 下载 APK

详见 [CI-CD.md § 3](CI-CD.md)。

## 4. 自托管 Runner

将 `.github/workflows/build.yml` 部署到自托管 runner：

```yaml
runs-on: self-hosted
```

适用于内网环境或自定义构建机。

## 5. 输出安装包

构建完成后 APK 可通过以下方式分发：

- GitHub Releases（自动）
- 自建分发服务
- CDN 直链
- 局域网共享（适用内网分发）

## 6. 验证 APK

```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging app-release.apk
```

确认包名、版本、权限。
