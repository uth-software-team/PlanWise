#!/usr/bin/env bash
# ==============================================================================
# Script: setup-dev.sh
# Project: PlanWise
# Description: Tự động khởi tạo môi trường phát triển (Dev Environment) cho dự án
# ==============================================================================

set -e

# Màu sắc thông báo
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Thư mục gốc dự án
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/Code/backend"
FRONTEND_DIR="${ROOT_DIR}/Code/frontend"

# Tham số mặc định
START_DOCKER=false
RUN_BACKEND=false
SKIP_BUILD=false

print_banner() {
  echo -e "${CYAN}${BOLD}"
  echo "╔════════════════════════════════════════════════════════════╗"
  echo "║                PlanWise Dev Environment Setup              ║"
  echo "║       Modular Monolith Backend & Modern Web System         ║"
  echo "╚════════════════════════════════════════════════════════════╝"
  echo -e "${NC}"
}

print_help() {
  echo -e "${BOLD}Cách sử dụng:${NC}"
  echo "  ./scripts/setup-dev.sh [OPTIONS]"
  echo ""
  echo -e "${BOLD}Các tùy chọn (Options):${NC}"
  echo "  -d, --docker       Tự động bật Docker container cho PostgreSQL (port 5432)"
  echo "  -r, --run          Khởi chạy Backend (bootRun) ngay sau khi thiết lập xong"
  echo "  -s, --skip-build   Bỏ qua bước verify compile backend"
  echo "  -h, --help         Hiển thị hướng dẫn này"
  echo ""
  echo -e "${BOLD}Ví dụ:${NC}"
  echo "  ./scripts/setup-dev.sh              # Cài đặt cơ bản (sử dụng H2 in-memory)"
  echo "  ./scripts/setup-dev.sh --docker     # Cài đặt và bật PostgreSQL container"
  echo "  ./scripts/setup-dev.sh -d -r        # Bật Postgres và chạy luôn backend"
  exit 0
}

# Parse CLI options
while [[ $# -gt 0 ]]; do
  case "$1" in
    -d|--docker)
      START_DOCKER=true
      shift
      ;;
    -r|--run)
      RUN_BACKEND=true
      shift
      ;;
    -s|--skip-build)
      SKIP_BUILD=true
      shift
      ;;
    -h|--help)
      print_help
      ;;
    *)
      echo -e "${RED}Lỗi: Tùy chọn không hợp lệ: $1${NC}"
      print_help
      ;;
  esac
done

print_banner

# ------------------------------------------------------------------------------
# 1. Kiểm tra môi trường (Prerequisites)
# ------------------------------------------------------------------------------
echo -e "${BLUE}${BOLD}==> [1/5] Kiểm tra công cụ & môi trường...${NC}"

# Kiểm tra Java
if command -v java >/dev/null 2>&1; then
  JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
  echo -e "  [✔] Java đã được cài đặt: ${GREEN}${JAVA_VER}${NC}"
else
  echo -e "  ${RED}[✘] Không tìm thấy Java trên máy!${NC}"
  echo -e "      Dự án yêu cầu Java 25."
  echo -e "      👉 Bạn có thể cài qua Homebrew: ${YELLOW}brew install openjdk@25${NC}"
  exit 1
fi

# Kiểm tra Docker
if command -v docker >/dev/null 2>&1; then
  echo -e "  [✔] Docker CLI đã sẵn sàng."
else
  echo -e "  ${YELLOW}[!] Không tìm thấy Docker. Bạn vẫn có thể dùng H2 Database in-memory.${NC}"
fi

# Kiểm tra Node.js (cho Frontend)
if command -v node >/dev/null 2>&1; then
  NODE_VER=$(node -v)
  echo -e "  [✔] Node.js đã sẵn sàng: ${GREEN}${NODE_VER}${NC}"
else
  echo -e "  ${YELLOW}[!] Không tìm thấy Node.js. Nếu phát triển Frontend, hãy cài Node.js (v18+).${NC}"
fi

# ------------------------------------------------------------------------------
# 2. Cấu hình biến môi trường (.env)
# ------------------------------------------------------------------------------
echo -e "\n${BLUE}${BOLD}==> [2/5] Thiết lập file cấu hình môi trường (.env)...${NC}"

if [ -d "${BACKEND_DIR}" ]; then
  BACKEND_ENV="${BACKEND_DIR}/.env"
  BACKEND_EXAMPLE="${BACKEND_DIR}/.env.example"

  if [ -f "${BACKEND_ENV}" ]; then
    echo -e "  [✔] File ${GREEN}Code/backend/.env${NC} đã tồn tại (giữ nguyên cấu hình hiện tại)."
  else
    if [ -f "${BACKEND_EXAMPLE}" ]; then
      cp "${BACKEND_EXAMPLE}" "${BACKEND_ENV}"
      # Tự động tạo ngẫu nhiên JWT_SECRET 32+ bytes nếu máy có openssl
      if command -v openssl >/dev/null 2>&1; then
        RAND_SECRET=$(openssl rand -base64 32 | tr -d '\n')
        # Thay thế JWT_SECRET mặc định trong file vừa tạo
        if [[ "$OSTYPE" == "darwin"* ]]; then
          sed -i '' "s/JWT_SECRET=.*/JWT_SECRET=${RAND_SECRET}/" "${BACKEND_ENV}"
        else
          sed -i "s/JWT_SECRET=.*/JWT_SECRET=${RAND_SECRET}/" "${BACKEND_ENV}"
        fi
        echo -e "  [✔] Đã tạo ${GREEN}Code/backend/.env${NC} từ .env.example (kèm auto-generated JWT Secret)."
      else
        echo -e "  [✔] Đã tạo ${GREEN}Code/backend/.env${NC} từ .env.example."
      fi
    else
      echo -e "  ${YELLOW}[!] Không tìm thấy Code/backend/.env.example, bỏ qua.${NC}"
    fi
  fi
fi

# Frontend .env nếu có .env.example
if [ -d "${FRONTEND_DIR}" ]; then
  FRONTEND_ENV="${FRONTEND_DIR}/.env"
  FRONTEND_EXAMPLE="${FRONTEND_DIR}/.env.example"
  if [ -f "${FRONTEND_EXAMPLE}" ] && [ ! -f "${FRONTEND_ENV}" ]; then
    cp "${FRONTEND_EXAMPLE}" "${FRONTEND_ENV}"
    echo -e "  [✔] Đã tạo ${GREEN}Code/frontend/.env${NC} từ .env.example."
  fi
fi

# ------------------------------------------------------------------------------
# 3. Phân quyền thực thi cho Gradle wrapper và Scripts
# ------------------------------------------------------------------------------
echo -e "\n${BLUE}${BOLD}==> [3/5] Cấp quyền thực thi (chmod +x)...${NC}"

if [ -f "${BACKEND_DIR}/gradlew" ]; then
  chmod +x "${BACKEND_DIR}/gradlew"
  echo -e "  [✔] Đã cấp quyền thực thi cho: ${GREEN}Code/backend/gradlew${NC}"
fi

chmod +x "${SCRIPT_DIR}"/*.sh 2>/dev/null || true
echo -e "  [✔] Đã cấp quyền thực thi cho các file trong thư mục ${GREEN}scripts/${NC}"

# ------------------------------------------------------------------------------
# 4. Quản lý Database (PostgreSQL qua Docker hoặc H2)
# ------------------------------------------------------------------------------
echo -e "\n${BLUE}${BOLD}==> [4/5] Cấu hình Cơ sở Dữ liệu (Database)...${NC}"

CONTAINER_NAME="planwise-postgres"

if [ "$START_DOCKER" = true ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo -e "  ${RED}[✘] Không tìm thấy Docker để khởi chạy container PostgreSQL.${NC}"
    echo -e "      Ứng dụng sẽ chuyển sang dùng H2 Database in-memory."
  elif ! docker info >/dev/null 2>&1; then
    echo -e "  ${RED}[✘] Docker daemon chưa được bật (Docker Desktop đang tắt).${NC}"
    echo -e "      Vui lòng mở Docker Desktop hoặc dùng H2 Database in-memory."
  else
    # Kiểm tra xem container đã tồn tại chưa
    if [ "$(docker ps -aq -f name=^${CONTAINER_NAME}$)" ]; then
      if [ "$(docker ps -q -f name=^${CONTAINER_NAME}$)" ]; then
        echo -e "  [✔] Docker container ${GREEN}${CONTAINER_NAME}${NC} đang chạy."
      else
        echo -e "  [i] Khởi động lại container ${CONTAINER_NAME}..."
        docker start "${CONTAINER_NAME}"
        echo -e "  [✔] Đã start container ${GREEN}${CONTAINER_NAME}${NC}."
      fi
    else
      echo -e "  [i] Đang kéo image và khởi tạo container PostgreSQL mới (${CONTAINER_NAME})..."
      docker run --name "${CONTAINER_NAME}" \
        -e POSTGRES_DB=planwise \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=postgres \
        -p 5432:5432 \
        -d postgres:16-alpine
      echo -e "  [✔] Đã tạo và khởi chạy container ${GREEN}${CONTAINER_NAME}${NC} thành công tại port 5432."
    fi
  fi
else
  echo -e "  [i] Chế độ mặc định: Sử dụng ${CYAN}H2 in-memory Database${NC} (không cần cài đặt DB)."
  echo -e "  💡 Muốn chạy PostgreSQL qua Docker? Dùng lệnh: ${YELLOW}./scripts/setup-dev.sh --docker${NC}"
fi

# ------------------------------------------------------------------------------
# 5. Kiểm tra build dự án
# ------------------------------------------------------------------------------
echo -e "\n${BLUE}${BOLD}==> [5/5] Kiểm tra và xác thực Backend build...${NC}"

if [ "$SKIP_BUILD" = true ]; then
  echo -e "  [i] Đã bỏ qua bước kiểm tra build (--skip-build)."
else
  if [ -d "${BACKEND_DIR}" ] && [ -f "${BACKEND_DIR}/gradlew" ]; then
    echo -e "  [i] Đang chạy kiểm tra biên dịch (compileJava)..."
    cd "${BACKEND_DIR}"
    ./gradlew compileJava --console=plain -q
    echo -e "  ${GREEN}[✔] Backend biên dịch thành công! Mã nguồn hoàn toàn hợp lệ.${NC}"
    cd "${ROOT_DIR}"
  fi
fi

# ------------------------------------------------------------------------------
# Hoàn tất & Hướng dẫn tiếp theo
# ------------------------------------------------------------------------------
echo -e "\n${GREEN}${BOLD}============================================================${NC}"
echo -e "${GREEN}${BOLD}         🎉 THIẾT LẬP MÔI TRƯỜNG DEV HOÀN TẤT! 🎉          ${NC}"
echo -e "${GREEN}${BOLD}============================================================${NC}"
echo ""
echo -e "${BOLD}Các lệnh phát triển hữu ích:${NC}"
echo -e "  1. Chạy Backend server:"
echo -e "     ${CYAN}cd Code/backend && ./gradlew bootRun${NC}"
echo ""
echo -e "  2. Swagger UI (Tài liệu API):"
echo -e "     ${CYAN}http://localhost:8080/api/swagger-ui.html${NC}"
echo ""
echo -e "  3. H2 Console (khi dùng H2 in-memory DB):"
echo -e "     ${CYAN}http://localhost:8080/api/h2-console${NC} (JDBC URL: jdbc:h2:mem:planwise)"
echo ""
echo -e "  4. Chạy toàn bộ Unit Tests:"
echo -e "     ${CYAN}cd Code/backend && ./gradlew test${NC}"
echo ""

if [ "$RUN_BACKEND" = true ]; then
  echo -e "${YELLOW}>> Đang tự động khởi chạy Backend server theo cờ --run...${NC}\n"
  cd "${BACKEND_DIR}"
  exec ./gradlew bootRun
fi
