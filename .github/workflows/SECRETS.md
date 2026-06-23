# GitHub Actions Secrets 配置指南

## 1. 必备 Secrets（仅签名 APK）

| Secret 名称 | 说明 | 获取方式 |
|------------|------|----------|
| `ANDROID_KEYSTORE_BASE64` | base64 编码的 keystore | `base64 -w 0 release.keystore` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 | 创建 keystore 时设置的密码 |
| `ANDROID_KEY_ALIAS` | 密钥别名 | 创建 keystore 时指定的别名 |
| `ANDROID_KEY_PASSWORD` | 密钥密码 | 密钥密码 |

### 生成 Keystore

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias wifibattle \
  -keyalg RSA -keysize 2048 \
  -validity 25000
```

### 编码为 base64

```bash
base64 -w 0 release.keystore | pbcopy   # macOS
base64 -w 0 release.keystore             # Linux
```

复制输出内容到 GitHub Secret。

## 2. 可选 Secrets - CDN / S3

| Secret 名称 | 说明 |
|------------|------|
| `AWS_ACCESS_KEY_ID` | AWS 访问密钥 |
| `AWS_SECRET_ACCESS_KEY` | AWS 密钥 |
| `AWS_REGION` | S3 所在区域，如 `us-east-1` |
| `S3_BUCKET` | S3 桶名，如 `myapp-releases` |

未配置时自动跳过 CDN 上传步骤。

## 3. 可选 Secrets - Google Play

| Secret 名称 | 说明 |
|------------|------|
| `PLAY_STORE_SERVICE_ACCOUNT` | Google Play API 服务账号 JSON 字符串 |

获取方式：Google Play Console → 设置 → API 访问 → 创建服务账号。

## 4. 可选 Secrets - 通知

| Secret 名称 | 说明 |
|------------|------|
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL |

## 5. 触发条件

| 事件 | 触发的工作流 |
|------|-------------|
| push 到 main | build, docker, pr-preview(若为 PR) |
| push 到 develop | build, pr-preview(若为 PR) |
| PR 到 main/develop | build, pr-preview |
| push tag `v*` | build + Release 发布 |
| 每日 UTC 02:00 | nightly |
| 手动 | build, docker, nightly |

## 6. 常见问题

**Q: 工作流没看到我的 Secret？**
A: 在 GitHub 仓库 → Settings → Secrets and variables → Actions 检查 secret 名称是否完全匹配（区分大小写）。

**Q: 如何手动触发？**
A: GitHub → Actions → 选择工作流 → Run workflow。

**Q: 如何下载构建产物？**
A: 进入 Actions → 选择 run → 底部 Artifacts 区域下载。

**Q: Release 中没有 APK？**
A: 确认 tag 格式为 `v*`（如 `v1.0.0`），且 build job 成功完成。
