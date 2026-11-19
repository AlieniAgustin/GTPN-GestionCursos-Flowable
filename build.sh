#!/usr/bin/env bash
# ==============================================================================
# Build Padre: Compilación y orquestación integral del sistema
# ==============================================================================
# Este script se encarga de compilar ambos módulos (flowable y backend),
# generar sus respectivos artefactos ejecutables (target/app.jar) y levantar
# el entorno completo con Docker Compose.
# ==============================================================================

set -euo pipefail

# Colores para salida de consola
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Directorio raíz del proyecto
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${ROOT_DIR}"

log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_step() {
    echo -e "\n${CYAN}==>${NC} ${GREEN}$1${NC}"
}

log_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
    echo -e "${RED}✖${NC} $1" >&2
}

show_help() {
    echo "Uso: ./build.sh [OPCIONES]"
    echo ""
    echo "Compila los módulos y levanta el stack completo de contenedores."
    echo ""
    echo "Opciones:"
    echo "  (sin args)         Compila flowable y backend, y levanta el stack (docker compose up --build -d)"
    echo "  -u, --up           Equivalente al comportamiento por defecto (compilar y levantar)"
    echo "  -b, --build-only   Solo compila los módulos Java (target/app.jar) sin levantar Docker"
    echo "  -s, --seed         Compila, levanta todo y ejecuta el script de seed de datos (seed-demo.sh)"
    echo "  -d, --down         Detiene y elimina los contenedores activos (docker compose down)"
    echo "      --clean        Detiene contenedores, elimina volúmenes (-v) y limpia carpetas target/"
    echo "  -h, --help         Muestra este mensaje de ayuda"
}

compile_module() {
    local module_name="$1"
    local module_path="${ROOT_DIR}/${module_name}"

    log_step "Compilando módulo: ${module_name}"

    if [ ! -f "${module_path}/build.sh" ]; then
        log_error "No se encontró el script de compilación en ${module_path}/build.sh"
        exit 1
    fi

    (
        cd "${module_path}"
        ./build.sh
    )

    if [ ! -f "${module_path}/target/app.jar" ]; then
        log_error "Fallo en la generación de ${module_name}/target/app.jar"
        exit 1
    fi

    log_info "Artefacto ${module_name}/target/app.jar verificado correctamente."
}

wait_for_services() {
    log_info "Esperando disponibilidad de servicios..."
    local retries=30
    local count=0

    # Esperar que PostgreSQL y Flowable respondan
    while [ $count -lt $retries ]; do
        if docker compose ps | grep -q "healthy"; then
            break
        fi
        sleep 2
        count=$((count + 1))
    done
}

print_summary() {
    echo -e "\n${GREEN}✔ Sistema compilado y levantado con éxito.${NC}\n"
    echo "Acceso a los componentes:"
    echo -e "  • ${CYAN}Backend Proxy / BFF:${NC}       http://localhost:8081/"
    echo -e "  • ${CYAN}Swagger UI (OpenAPI):${NC}       http://localhost:8081/swagger-ui.html"
    echo -e "  • ${CYAN}Flowable Engine (REST):${NC}     http://localhost:8080/ (admin / test)"
    echo -e "  • ${CYAN}Flowable UI:${NC}                http://localhost:8088/flowable-ui/ (admin / test)"
    echo -e "  • ${CYAN}PostgreSQL Database:${NC}        localhost:5432 (user: flowable, db: flowable)"
    echo ""
    echo "Comandos útiles:"
    echo "  Ver logs en vivo:            docker compose logs -f"
    echo "  Cargar datos de prueba:      ./scripts/seed-demo.sh 8"
    echo "  Detener el entorno:          ./build.sh --down"
    echo ""
}

# Procesamiento de argumentos
MODE="up"
SEED_AFTER=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -u|--up)
            MODE="up"
            shift
            ;;
        -b|--build-only)
            MODE="build-only"
            shift
            ;;
        -s|--seed)
            MODE="up"
            SEED_AFTER=true
            shift
            ;;
        -d|--down)
            MODE="down"
            shift
            ;;
        --clean)
            MODE="clean"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Opción no reconocida: $1"
            show_help
            exit 1
            ;;
    esac
done

# Ejecución según modo
case "${MODE}" in
    down)
        log_step "Deteniendo stack de Docker Compose..."
        docker compose down
        log_info "Contenedores detenidos."
        exit 0
        ;;
    clean)
        log_step "Limpiando entorno y artefactos..."
        docker compose down -v --remove-orphans || true
        rm -rf "${ROOT_DIR}/flowable/target" "${ROOT_DIR}/backend/target"
        log_info "Entorno limpio."
        exit 0
        ;;
    build-only)
        compile_module "flowable"
        compile_module "backend"
        echo -e "\n${GREEN}✔ Ambos módulos compilados exitosamente (target/app.jar listos).${NC}"
        exit 0
        ;;
    up)
        # 1. Compilar submódulos
        compile_module "flowable"
        compile_module "backend"

        # 2. Levantar Docker Compose
        log_step "Levantando servicios con Docker Compose..."
        docker compose up --build -d

        # 3. Resumen y URLs
        print_summary

        # 4. Ejecución opcional de Seed
        if [ "${SEED_AFTER}" = true ]; then
            log_step "Ejecutando seed de datos de demostración..."
            sleep 5
            "${ROOT_DIR}/scripts/seed-demo.sh" 8
        fi
        ;;
esac
