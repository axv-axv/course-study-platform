#!/bin/bash
# 知学 · 课程资料与智能学习平台 启动器（macOS / Linux）
cd "$(dirname "$0")"

echo "============================================"
echo "   知学 · 课程资料与智能学习平台 启动器"
echo "============================================"
echo

# 1. 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "[错误] 未检测到 Node.js，请先安装："
    echo "       打开浏览器访问 https://nodejs.org"
    echo "       下载 LTS 版本并安装"
    echo
    read -p "按回车键退出..." -r
    exit 1
fi

# 2. 首次运行安装依赖
if [ ! -d node_modules ]; then
    echo "[1/3] 首次运行，正在安装依赖（约需 1-3 分钟）..."
    if ! npm install; then
        echo
        echo "[错误] 依赖安装失败，请检查网络后重新双击本文件"
        read -p "按回车键退出..." -r
        exit 1
    fi
fi

# 3. 启动并自动打开浏览器
echo "[2/3] 正在启动前端服务..."
echo "[3/3] 稍后会自动打开浏览器：http://localhost:5173"
echo
echo "提示：关闭本终端即停止服务；窗口请保持开启，不要关闭。"
echo
(sleep 5 && open http://localhost:5173 2>/dev/null || true) &
npm run dev

echo
echo "服务已停止。"
