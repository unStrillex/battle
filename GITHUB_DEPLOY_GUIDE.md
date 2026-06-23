# WiFi Battle Platform - GitHub 发布操作手册

本文档逐步指导您将 `D:\gitzzz\wifi-battle-platform` 项目发布到 GitHub，并启用完整的 CI/CD 流水线。

---

## 第 1 步：创建 GitHub Personal Access Token (PAT)

> Token 是您与 GitHub 通信的"密码",**只会显示一次**,请妥善保存。

### 1.1 打开 Token 创建页面

在浏览器中打开: <https://github.com/settings/tokens/new>

### 1.2 填写表单

| 字段 | 推荐值 |
|------|--------|
| **Note (备注)** | `WiFi Battle Platform Deploy` |
| **Expiration (过期)** | `No expiration` 或 `90 days`(推荐) |
| **Resource owner** | 您的个人账号 |
| **Repository access** | `All repositories` 或 `Only select repositories` → 选后续要建的 `wifi-battle-platform` |

### 1.3 勾选权限 (Scopes)

请勾选以下**必需**权限,本项目只需要 `repo` 即可:

- [x] **repo** (完整仓库访问,包含读写)

可选权限(后续如需发布 Docker / Play Store 才需要):
- [ ] `write:packages` - 发布 GHCR Docker 镜像
- [ ] `read:org` - 组织级操作
- [ ] `admin:repo_hook` - Webhook 管理

### 1.4 点击底部绿色按钮

`Generate token` → 复制形如 `ghp_xxxxxxxxxxxxxxxxxxxx` 的字符串。

> ⚠️ **重要**:此 token 只显示一次!请立即复制到剪贴板,或保存到密码管理器。

---

## 第 2 步:在 GitHub 创建空仓库

### 2.1 打开创建页面

<https://github.com/new>

### 2.2 填写仓库信息

| 字段 | 值 |
|------|----|
| **Repository name** | `wifi-battle-platform` |
| **Description** | `Android WiFi LAN multiplayer battle platform framework - 多人对战框架` |
| **Visibility** | `Public` (推荐,享受免费 GitHub Actions) |
| **Initialize** | **全部不要勾选**(不要 README / .gitignore / license,本地已有) |

### 2.3 点击 `Create repository`

记下页面显示的仓库地址,形如:
- HTTPS: `https://github.com/<您的用户名>/wifi-battle-platform.git`
- SSH: `git@github.com:<您的用户名>/wifi-battle-platform.git`

---

## 第 3 步:本地推送项目

打开 PowerShell,执行以下命令(我已经把全部命令准备好,您只需替换 `<TOKEN>` 和 `<USERNAME>`):

```powershell
# 进入项目目录
cd D:\gitzzz\wifi-battle-platform

# 初始化 Git 仓库(若已存在会报错,可忽略)
git init -b main

# 配置提交者身份(请改为您的 GitHub 用户名和邮箱)
git config user.name "Your Name"
git config user.email "your-email@example.com"

# 暂存所有文件
git add .

# 首次提交
git commit -m "feat: initial commit - WiFi Battle Platform v1.0.0"

# 关联远程仓库(请将 <USERNAME> 替换为您的 GitHub 用户名)
git remote add origin https://github.com/<USERNAME>/wifi-battle-platform.git

# 推送到 GitHub
# 当提示输入凭据时:
#   Username: <USERNAME>
#   Password: <TOKEN>     <-- 这里粘贴刚才生成的 Token,不是 GitHub 密码!
git push -u origin main
```

### 3.1 推送成功的标志

终端会显示:
```
Enumerating objects: 85, done.
Counting objects: 100% (85/85), done.
...
To https://github.com/<USERNAME>/wifi-battle-platform.git
 * [new branch]      main -> main
Branch 'main' set up to track remote branch 'main' from 'origin'.
```

### 3.2 凭据缓存(可选,避免每次输入)

```powershell
# Windows 凭据管理器会自动记住 Token
# 重新设置可使用:
git config --global credential.helper manager-core
```

---

## 第 4 步:验证 CI/CD 流水线

推送完成后,项目自带的 GitHub Actions 会**自动开始构建**。

### 4.1 查看构建状态

打开: `https://github.com/<USERNAME>/wifi-battle-platform/actions`

应该看到 4 个工作流:
- `Android Build` - 主构建
- `Docker` - Docker 镜像构建
- `Nightly Build` - 夜间构建
- `PR Preview` - PR 预览

### 4.2 下载构建产物

`Actions` → 选择一次成功的 run → 底部 `Artifacts`:
- `app-debug` - Debug APK
- `app-release-unsigned` - 未签名 Release APK
- `lint-report` - 代码检查报告
- `test-results` - 单元测试结果

---

## 第 5 步:发布第一个正式版本

### 5.1 配置签名(可选,推荐)

如果您需要发布可安装的 Release APK,需先生成 keystore:

```powershell
# 1. 生成 keystore
keytool -genkey -v `
  -keystore release.keystore `
  -alias wifibattle `
  -keyalg RSA -keysize 2048 `
  -validity 25000 `
  -storepass <YOUR_STORE_PASSWORD> `
  -keypass <YOUR_KEY_PASSWORD> `
  -dname "CN=WiFi Battle, OU=Dev, O=Personal, L=City, ST=State, C=CN"

# 2. 编码为 base64
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Content keystore.b64
```

在 GitHub 仓库 → `Settings` → `Secrets and variables` → `Actions` → `New repository secret`,依次添加:

| Secret 名 | 值 |
|-----------|---|
| `ANDROID_KEYSTORE_BASE64` | `keystore.b64` 文件内容 |
| `ANDROID_KEYSTORE_PASSWORD` | 您的 keystore 密码 |
| `ANDROID_KEY_ALIAS` | `wifibattle` |
| `ANDROID_KEY_PASSWORD` | 您的 key 密码 |

### 5.2 推送 tag 触发 Release

```powershell
cd D:\gitzzz\wifi-battle-platform
git tag v1.0.0
git push origin v1.0.0
```

几分钟后,GitHub 会自动:
1. 构建签名版 APK
2. 创建 GitHub Release,附带 APK 下载链接

---

## 常见问题

**Q: 推送时报错 `remote: Support for password authentication was removed`**
A: 这正是改用 Token 的原因。请确认 Password 字段粘贴的是 Token(以 `ghp_` 开头),而不是 GitHub 登录密码。

**Q: Actions 卡在 `Set up job` 很久**
A: 公共仓库免费 2000 分钟/月,首次运行需下载依赖,可能需要 5-10 分钟。

**Q: 想要撤销 Token**
A: <https://github.com/settings/tokens> → 点击 Token → `Delete`。

**Q: 推送时提示 `Permission denied (publickey)`**
A: 切换为 HTTPS 方式,或配置 SSH key: <https://github.com/settings/keys>。

---

## 一句话总结

**只需 3 步**:
1. 创建 Token → 复制保存
2. 在 GitHub 创建空仓库 `wifi-battle-platform`
3. 跑完本目录的 `push-to-github.ps1` 脚本,粘贴 Token,搞定
