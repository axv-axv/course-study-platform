@echo off
chcp 65001 >nul
title 知学平台启动器
echo ============================================
echo    知学 · 课程资料与智能学习平台 启动器
echo ============================================
echo.

rem ---------- 1. 检查 Node.js ----------
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Node.js，请先安装：
    echo        打开浏览器访问 https://nodejs.org
    echo        下载左侧 LTS 版本并安装（一路下一步即可）
    echo.
    pause
    exit /b
)

rem ---------- 2. 首次运行安装依赖 ----------
if not exist node_modules (
    echo [1/3] 首次运行，正在安装依赖（约需 1-3 分钟）...
    call npm install
    if %errorlevel% neq 0 (
        echo.
        echo [错误] 依赖安装失败，请检查网络后重新双击本文件
        pause
        exit /b
    )
)

rem ---------- 3. 启动并自动打开浏览器 ----------
echo [2/3] 正在启动前端服务...
echo [3/3] 稍后会自动打开浏览器：http://localhost:5173
echo.
echo 提示：关闭本窗口即停止服务；窗口请保持开启，不要关闭。
echo.
start /b cmd /c "timeout /t 6 /nobreak >nul && start http://localhost:5173"
call npm run dev

echo.
echo 服务已停止。
pause
