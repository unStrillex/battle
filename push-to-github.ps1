# ==============================================================
# WiFi Battle Platform - 一键推送 GitHub 脚本
# 使用方法:
#   1. 在 GitHub 创建空仓库 https://github.com/new (不要勾选 Initialize)
#   2. 在 https://github.com/settings/tokens/new 创建 Token (勾选 repo)
#   3. 以管理员权限打开 PowerShell,运行本脚本:
#      .\push-to-github.ps1 -GitHubUser "your-username" -Token "ghp_xxxx"
# ==============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUser,

    [Parameter(Mandatory=$true)]
    [string]$Token,

    [string]$UserName = "WiFi Battle Developer",
    [string]$UserEmail = "dev@wifibattle.local",
    [string]$Branch = "main",
    [string]$RepoName = "wifi-battle-platform"
)

# 颜色输出
function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Err($msg)  { Write-Host "    [ERR] $msg" -ForegroundColor Red }
function Write-Warn($msg) { Write-Host "    [WARN] $msg" -ForegroundColor Yellow }

$ErrorActionPreference = "Stop"

# --------------------------------------------------------------
# 0. 准备:确保 git 可用
# --------------------------------------------------------------
Write-Step "Step 0/6  - 验证 Git 环境"
$gitPath = (Get-Command git -ErrorAction SilentlyContinue).Path
if (-not $gitPath) {
    # 尝试刷新 PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    $gitPath = (Get-Command git -ErrorAction SilentlyContinue).Path
}
if (-not $gitPath) {
    Write-Err "未找到 Git,正在自动安装..."
    winget install --id Git.Git -e --source winget --accept-package-agreements --accept-source-agreements
    Write-Ok "Git 安装完成,请重新打开 PowerShell 再运行本脚本"
    exit 0
}
Write-Ok "Git: $(git --version)"

# --------------------------------------------------------------
# 1. 切换到项目根目录
# --------------------------------------------------------------
Write-Step "Step 1/6  - 切换到项目目录"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir
Write-Ok "当前目录: $(Get-Location)"

# --------------------------------------------------------------
# 2. 配置 Git 用户信息
# --------------------------------------------------------------
Write-Step "Step 2/6  - 配置 Git 用户信息"
git config user.name  $UserName
git config user.email $UserEmail
Write-Ok "user.name  = $UserName"
Write-Ok "user.email = $UserEmail"

# --------------------------------------------------------------
# 3. 初始化仓库(若已存在则跳过)
# --------------------------------------------------------------
Write-Step "Step 3/6  - 初始化本地仓库"
if (-not (Test-Path ".git")) {
    git init -b $Branch | Out-Null
    Write-Ok "已初始化分支 $Branch"
} else {
    Write-Ok "仓库已存在,跳过初始化"
}

# 检查分支
$currentBranch = git branch --show-current
if ($currentBranch -ne $Branch) {
    Write-Warn "当前分支为 '$currentBranch',切换到 '$Branch'"
    git checkout -B $Branch 2>&1 | Out-Null
}

# --------------------------------------------------------------
# 4. 暂存并提交
# --------------------------------------------------------------
Write-Step "Step 4/6  - 添加并提交代码"
git add .

$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Ok "工作区干净,无新文件需要提交"
} else {
    $fileCount = ($status -split "`n").Count
    Write-Ok "暂存了 $fileCount 个变更"
    git commit -m "feat: initial commit - WiFi Battle Platform v1.0.0" | Out-Null
    Write-Ok "提交完成"
}

# --------------------------------------------------------------
# 5. 关联远程仓库
# --------------------------------------------------------------
Write-Step "Step 5/6  - 关联远程仓库"
$remoteUrl = "https://${Token}@github.com/${GitHubUser}/${RepoName}.git"

$existing = git remote get-url origin 2>$null
if ($existing) {
    Write-Warn "检测到现有 remote: $existing"
    git remote set-url origin $remoteUrl
    Write-Ok "已更新 remote URL"
} else {
    git remote add origin $remoteUrl
    Write-Ok "已添加 remote origin"
}

# --------------------------------------------------------------
# 6. 推送到 GitHub
# --------------------------------------------------------------
Write-Step "Step 6/6  - 推送到 GitHub ($GitHubUser/$RepoName)"
try {
    git push -u origin $Branch --force
    Write-Ok "推送成功!"
} catch {
    Write-Err "推送失败: $_"
    Write-Host ""
    Write-Host "可能原因:" -ForegroundColor Yellow
    Write-Host "  1. Token 权限不足 (需要勾选 'repo')"
    Write-Host "  2. GitHub 上尚未创建仓库 https://github.com/new"
    Write-Host "  3. 网络问题"
    exit 1
}

# --------------------------------------------------------------
# 完成
# --------------------------------------------------------------
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  发布成功!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "  仓库地址: https://github.com/$GitHubUser/$RepoName" -ForegroundColor Cyan
Write-Host "  Actions:  https://github.com/$GitHubUser/$RepoName/actions" -ForegroundColor Cyan
Write-Host ""
Write-Host "  接下来可以:" -ForegroundColor Yellow
Write-Host "    1. 打开 Actions 查看自动构建进度"
Write-Host "    2. 配置签名 Secrets (参考 GITHUB_DEPLOY_GUIDE.md 第 5 步)"
Write-Host "    3. 推送 tag 发布版本: git tag v1.0.0 && git push origin v1.0.0"
Write-Host ""
