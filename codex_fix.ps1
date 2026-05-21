# Codex 401 错误诊断修复脚本
# 保存为 codex_fix.ps1，右键选择"使用 PowerShell 运行"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   Codex 401 错误诊断工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查当前目录的 .env 文件
Write-Host "[1/6] 检查项目中的 .env 文件..." -ForegroundColor Yellow
$envFile = ".\.env"
if (Test-Path $envFile) {
    Write-Host "⚠️  发现 .env 文件！内容如下：" -ForegroundColor Red
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "OPENAI_API_KEY") {
            Write-Host "   $_" -ForegroundColor Red
        } else {
            Write-Host "   $_"
        }
    }
    Write-Host ""
    $rename = Read-Host "是否将 .env 重命名为 .env.backup 以避免冲突？(y/n)"
    if ($rename -eq "y") {
        Rename-Item $envFile ".env.backup"
        Write-Host "✓ 已重命名为 .env.backup" -ForegroundColor Green
    }
} else {
    Write-Host "✓ 未发现 .env 文件" -ForegroundColor Green
}
Write-Host ""

# 2. 检查环境变量
Write-Host "[2/6] 检查环境变量..." -ForegroundColor Yellow
$openaiKey = $env:OPENAI_API_KEY
$codexKey = $env:CODEX_API_KEY
$baseUrl = $env:OPENAI_BASE_URL

if ($openaiKey) {
    Write-Host "✓ OPENAI_API_KEY 已设置" -ForegroundColor Green
} else {
    Write-Host "✗ OPENAI_API_KEY 未设置" -ForegroundColor Red
}

if ($codexKey) {
    Write-Host "✓ CODEX_API_KEY 已设置" -ForegroundColor Green
} else {
    Write-Host "○ CODEX_API_KEY 未设置（可选）" -ForegroundColor Gray
}

if ($baseUrl) {
    Write-Host "⚠️  OPENAI_BASE_URL 已设置: $baseUrl" -ForegroundColor Red
    Write-Host "    这可能导致 401 错误！" -ForegroundColor Red
    $removeBaseUrl = Read-Host "是否删除 OPENAI_BASE_URL？(y/n)"
    if ($removeBaseUrl -eq "y") {
        Remove-Item Env:\OPENAI_BASE_URL
        Write-Host "✓ 已删除 OPENAI_BASE_URL" -ForegroundColor Green
    }
} else {
    Write-Host "✓ OPENAI_BASE_URL 未设置" -ForegroundColor Green
}
Write-Host ""

# 3. 检查 Codex 缓存
Write-Host "[3/6] 检查 Codex 缓存..." -ForegroundColor Yellow
$codexDir = "$env:USERPROFILE\.codex"
$authFile = "$codexDir\auth.json"
$configFile = "$codexDir\config.toml"

if (Test-Path $authFile) {
    Write-Host "⚠️  发现 auth.json 缓存文件" -ForegroundColor Yellow
    $clearCache = Read-Host "是否清除 auth.json 缓存？(y/n)"
    if ($clearCache -eq "y") {
        Remove-Item $authFile -Force
        Write-Host "✓ 已清除缓存" -ForegroundColor Green
    }
} else {
    Write-Host "✓ 无 auth.json 缓存" -ForegroundColor Green
}

# 4. 创建配置文件
Write-Host ""
Write-Host "[4/6] 检查 Codex 配置..." -ForegroundColor Yellow
if (-not (Test-Path $codexDir)) {
    New-Item -ItemType Directory -Path $codexDir -Force | Out-Null
    Write-Host "✓ 创建 .codex 目录" -ForegroundColor Green
}

if (Test-Path $configFile) {
    Write-Host "✓ 发现 config.toml" -ForegroundColor Green
    $content = Get-Content $configFile -Raw
    if ($content -match "preferred_auth_method") {
        Write-Host "✓ 已配置 preferred_auth_method" -ForegroundColor Green
    } else {
        Write-Host "⚠️  未配置 preferred_auth_method" -ForegroundColor Yellow
        $addConfig = Read-Host "是否添加 API Key 优先配置？(y/n)"
        if ($addConfig -eq "y") {
            Add-Content $configFile "`npreferred_auth_method = `"apikey`"" -Encoding UTF8
            Write-Host "✓ 已添加配置" -ForegroundColor Green
        }
    }
} else {
    Write-Host "⚠️  未找到 config.toml，创建中..." -ForegroundColor Yellow
    @"
preferred_auth_method = "apikey"
"@ | Set-Content $configFile -Encoding UTF8
    Write-Host "✓ 已创建 config.toml 并配置 API Key 优先" -ForegroundColor Green
}
Write-Host ""

# 5. 测试 API Key 调用 Codex
Write-Host "[5/6] 测试 API Key 是否可以调用 Codex..." -ForegroundColor Yellow
if ($openaiKey) {
    try {
        $response = Invoke-RestMethod -Uri "https://api.openai.com/v1/responses" `
            -Method Post `
            -Headers @{
                "Authorization" = "Bearer $openaiKey"
                "Content-Type" = "application/json"
            } `
            -Body '{"model": "gpt-4.1", "input": "test"}' `
            -ErrorAction Stop
        Write-Host "✓ API Key 可以调用 Codex API" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 401) {
            Write-Host "✗ API Key 无法调用 Codex API（401 错误）" -ForegroundColor Red
            Write-Host "  这说明你的 API Key 没有 Codex 权限" -ForegroundColor Red
        } elseif ($statusCode -eq 404) {
            Write-Host "✓ API Key 可以调用 API（404 是因为模型名可能不对，但认证通过）" -ForegroundColor Green
        } else {
            Write-Host "? 返回状态码: $statusCode" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "✗ 未设置 OPENAI_API_KEY，跳过测试" -ForegroundColor Red
}
Write-Host ""

# 6. 总结和建议
Write-Host "[6/6] 总结" -ForegroundColor Yellow
Write-Host ""
Write-Host "修复完成！请按以下步骤操作：" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 完全关闭 VS Code（确保进程已结束）" -ForegroundColor White
Write-Host "2. 从 PowerShell 启动 VS Code：" -ForegroundColor White
Write-Host "   code" -ForegroundColor Green
Write-Host "3. 在 VS Code 中打开 Codex 面板" -ForegroundColor White
Write-Host "4. 选择 API Key 方式登录" -ForegroundColor White
Write-Host ""
Write-Host "如果仍然 401，说明你的 API Key 没有 Codex 权限" -ForegroundColor Red
Write-Host "需要访问 https://platform.openai.com 申请 Codex API 访问" -ForegroundColor Red
Write-Host ""
Read-Host "按 Enter 键退出"
