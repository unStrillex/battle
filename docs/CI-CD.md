# GitHub Actions 部署指南

本项目使用 GitHub Actions 实现完整的 CI/CD 流水线。

## 工作流总览

项目共提供 **4 个工作流**，覆盖构建、发布、预览、监控全流程。

| 工作流 | 触发条件 | 用途 |
|--------|----------|------|
| `build.yml` | push / tag / PR / 手动 | 完整构建 + Lint + Test + Release |
| `docker.yml` | push main / tag / 手动 | 构建并发布 Docker 镜像到 GHCR |
| `nightly.yml` | 每日 02:00 UTC / 手动 | 夜间构建，提前发现问题 |
| `pr-preview.yml` | PR 创建/更新 | PR 预览 APK + 评论链接 |

## 1. 快速开始

### 1.1 推送触发

```bash
git add .
git commit -m "feat: 新功能"
git push origin main   # 触发 build.yml
```

### 1.2 发布版本

```bash
git tag v1.0.0
git push origin v1.0.0   # 触发 build.yml + 自动创建 GitHub Release
```

### 1.3 手动触发

1. 打开 GitHub → Actions
2. 选择工作流（如 `Android Build`）
3. 点击 `Run workflow`
4. 填写参数：
   - `build_type`: release / debug
   - `upload_to_cdn`: true / false
5. 点击 `Run workflow` 按钮

## 2. 配置签名

参考 `SECRETS.md` 在 GitHub 仓库配置以下 4 个 Secret：

```
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

配置后所有 Release 构建会自动签名。

## 3. 下载 APK

### 3.1 Actions Artifacts（所有构建）

```
GitHub → Actions → 选择 run → 底部 Artifacts
```

### 3.2 GitHub Releases（tag 触发）

```
GitHub → Releases → 选择版本 → Assets
```

### 3.3 PR 预览包

PR 页面的 bot 评论中包含下载链接，保留 14 天。

## 4. 部署到 CDN（S3）

### 4.1 配置 Secrets

| Secret | 值 |
|--------|-----|
| `AWS_ACCESS_KEY_ID` | IAM 用户访问密钥 |
| `AWS_SECRET_ACCESS_KEY` | IAM 用户密钥 |
| `AWS_REGION` | S3 区域 |
| `S3_BUCKET` | 桶名 |

### 4.2 创建 S3 桶并配置公开读

```bash
aws s3api create-bucket --bucket myapp-releases --region us-east-1
aws s3api put-bucket-policy --bucket myapp-releases --policy file://policy.json
```

`policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicRead",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::myapp-releases/*"
  }]
}
```

### 4.3 下载链接格式

```
https://<bucket>.s3.amazonaws.com/wifibattle/v1.0.0/app-release.apk
https://<bucket>.s3.amazonaws.com/wifibattle/latest/app-release.apk
```

## 5. 部署到 Google Play

### 5.1 创建服务账号

1. Google Play Console → 设置 → API 访问
2. 创建新服务账号，授予"发布"权限
3. 下载 JSON 密钥

### 5.2 配置 Secret

将整个 JSON 文件内容复制到 Secret `PLAY_STORE_SERVICE_ACCOUNT`。

### 5.3 触发

推送 tag `v*` 自动上传到 Play Internal Track。

## 6. Docker 镜像

每次 push 到 main 会自动构建并推送到 GitHub Container Registry：

```
ghcr.io/<owner>/<repo>/android-builder:latest
ghcr.io/<owner>/<repo>/android-builder:1.0.0
```

可在其他项目引用此镜像加速构建。

## 7. 调试技巧

### 7.1 SSH 调试

在 Actions 页面如果构建失败，可以使用 [act](https://github.com/nektos/act) 本地复现：

```bash
brew install act
act -j build
```

### 7.2 启用 SSH 调试

在 workflow 中临时添加：

```yaml
- name: Setup tmate session
  if: failure()
  uses: mxschmitt/action-tmate@v3
```

### 7.3 查看详细日志

在 Job 页面点击步骤可展开详细日志。

## 8. 性能优化

项目已默认启用：
- ✅ Gradle 缓存（基于 hash）
- ✅ Docker Buildx 缓存
- ✅ Lint/Test/Build 并行 Job
- ✅ 自动取消旧运行（concurrency group）

如需进一步加速可考虑：
- 使用自托管 runner
- 拆分 Gradle 模块启用并行编译
- 使用 ECR/GHCR 缓存 Gradle 依赖

## 9. 安全建议

- ⚠️ Keystore 永远不要提交到代码仓库
- ⚠️ 定期轮换 AWS / GCP 凭证
- ⚠️ 启用 `GITHUB_TOKEN` 权限最小化
- ⚠️ 在 Settings → Code security 启用 Dependabot 与 secret scanning
