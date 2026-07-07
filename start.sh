#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.local/logs"
PID_DIR="$ROOT_DIR/.local/pids"

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-kk112233}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
RUN_SQL="${RUN_SQL:-1}"
SKIP_NPM_INSTALL="${SKIP_NPM_INSTALL:-0}"

BACKEND_PID=""
FRONTEND_PID=""
MYSQL_ARGS=(-u "$MYSQL_USER")
if [[ -n "$MYSQL_PASSWORD" ]]; then
  MYSQL_ARGS+=("-p${MYSQL_PASSWORD}")
fi

mkdir -p "$LOG_DIR" "$PID_DIR"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令：$1"
    exit 1
  fi
}

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

print_port_owner() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN || true
}

cleanup() {
  local code=$?
  trap - EXIT INT TERM

  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    pkill -TERM -P "$FRONTEND_PID" >/dev/null 2>&1 || true
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi

  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    pkill -TERM -P "$BACKEND_PID" >/dev/null 2>&1 || true
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi

  exit "$code"
}

trap cleanup EXIT INT TERM

require_cmd java
require_cmd mvn
require_cmd npm
require_cmd mysql
require_cmd mysqladmin
require_cmd lsof

echo "检查 MySQL..."
mysqladmin "${MYSQL_ARGS[@]}" ping >/dev/null

if [[ "$RUN_SQL" != "0" ]]; then
  echo "导入数据库脚本..."
  for file in "$ROOT_DIR"/sql/0*.sql; do
    mysql "${MYSQL_ARGS[@]}" < "$file"
  done
else
  echo "跳过数据库导入：RUN_SQL=0"
fi

if port_in_use "$BACKEND_PORT"; then
  echo "后端端口 $BACKEND_PORT 已被占用："
  print_port_owner "$BACKEND_PORT"
  exit 1
fi

if port_in_use "$FRONTEND_PORT"; then
  echo "前端端口 $FRONTEND_PORT 已被占用："
  print_port_owner "$FRONTEND_PORT"
  exit 1
fi

echo "打包后端..."
(cd "$ROOT_DIR/backend" && mvn package -DskipTests)

if [[ "$SKIP_NPM_INSTALL" != "1" || ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "安装前端依赖..."
  (cd "$ROOT_DIR/frontend" && npm install)
else
  echo "跳过前端依赖安装：SKIP_NPM_INSTALL=1"
fi

echo "启动后端：http://127.0.0.1:$BACKEND_PORT"
(cd "$ROOT_DIR/backend" && java -jar app/target/app-1.0.0.jar --server.port="$BACKEND_PORT" > "$LOG_DIR/backend.log" 2>&1) &
BACKEND_PID=$!
echo "$BACKEND_PID" > "$PID_DIR/backend.pid"

echo "启动前端：http://localhost:$FRONTEND_PORT"
(cd "$ROOT_DIR/frontend" && npm run dev -- --host 0.0.0.0 --port "$FRONTEND_PORT" > "$LOG_DIR/frontend.log" 2>&1) &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$PID_DIR/frontend.pid"

echo
echo "项目已启动："
echo "  前端：http://localhost:$FRONTEND_PORT"
echo "  后端：http://127.0.0.1:$BACKEND_PORT"
echo "  后端日志：$LOG_DIR/backend.log"
echo "  前端日志：$LOG_DIR/frontend.log"
echo
echo "按 Ctrl+C 停止本脚本启动的服务。"

wait "$BACKEND_PID" "$FRONTEND_PID"
