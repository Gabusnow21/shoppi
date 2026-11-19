
#!/usr/bin/env bash
#
# Prepara el entorno local antes de compilar/instalar la app desde Android Studio.
#
# 1. Levanta la API Spring Boot en segundo plano si todavía no responde.
# 2. Configura adb reverse tcp:8080 tcp:8080 en cada dispositivo conectado.
#
# Compatible con Windows/Git Bash, Linux y macOS.
#
# Variables opcionales:
#   SHOPPI_SKIP_DEV_BACKEND=1
#   DB_URL, DB_USER, DB_PASS
#   SHOPPI_BACKEND_LOG
#   SHOPPI_BACKEND_PID
#   SHOPPI_HEALTH_URL
#
set -euo pipefail

ANDROID_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$(cd "$ANDROID_DIR/.." && pwd)"
BACKEND_DIR="$REPO_DIR/backend"

LOG_FILE="${SHOPPI_BACKEND_LOG:-/tmp/shoppi-backend.log}"
PID_FILE="${SHOPPI_BACKEND_PID:-/tmp/shoppi-backend.pid}"
HEALTH_URL="${SHOPPI_HEALTH_URL:-http://127.0.0.1:8080/api/ping}"

log() {
    printf '[shoppi-dev] %s\n' "$*"
}

# Convierte rutas Windows a rutas compatibles con Git Bash.
convertir_ruta() {
    local ruta="$1"

    if command -v cygpath >/dev/null 2>&1; then
        cygpath -u "$ruta"
    else
        printf '%s\n' "$ruta"
    fi
}

if [[ "${SHOPPI_SKIP_DEV_BACKEND:-0}" == "1" ]]; then
    log "SHOPPI_SKIP_DEV_BACKEND=1: no se inicia la API."
    exit 0
fi

# Evita dos arranques simultáneos.
LOCK_FILE="${TMPDIR:-/tmp}/shoppi-dev-backend.lock"
exec 9>"$LOCK_FILE"

if command -v flock >/dev/null 2>&1; then
    flock 9
fi

backend_arriba() {
    curl -fsS --max-time 2 "$HEALTH_URL" >/dev/null 2>&1
}

configurar_db() {
    if [[ -n "${DB_URL:-}" ]]; then
        return
    fi

    local puerto=5432

    if command -v pg_isready >/dev/null 2>&1 &&
        pg_isready -h 127.0.0.1 -p 5433 -q 2>/dev/null; then
        puerto=5433
    fi

    export DB_URL="jdbc:postgresql://127.0.0.1:${puerto}/shoppi"
    export DB_USER="${DB_USER:-shoppi}"
    export DB_PASS="${DB_PASS:-shoppi}"

    log "PostgreSQL detectado en el puerto ${puerto}."
}

cargar_firebase_service_account() {
    if [[ -n "${FIREBASE_SERVICE_ACCOUNT_JSON:-}" ]]; then
        log "FIREBASE_SERVICE_ACCOUNT_JSON ya definido en el entorno."
        return
    fi

    local local_props="$ANDROID_DIR/local.properties"

    if [[ ! -f "$local_props" ]]; then
        log "AVISO: no se encontró local.properties."
        return
    fi

    local valor

    valor="$(sed -n 's/^shoppi[.]firebaseServiceAccount=//p' "$local_props" | tail -n 1)"

    if [[ -z "$valor" ]]; then
        log "ERROR: falta shoppi.firebaseServiceAccount en $local_props."
        return 1
    fi

    valor="$(convertir_ruta "$valor")"

    if [[ ! -f "$valor" ]]; then
        log "ERROR: no existe el archivo de cuenta de servicio: $valor"
        return 1
    fi

    export FIREBASE_SERVICE_ACCOUNT_JSON="$valor"

    log "FIREBASE_SERVICE_ACCOUNT_JSON cargado desde local.properties."
}

iniciar_api() {
    if backend_arriba; then
        log "La API ya está arriba en http://localhost:8080."
        return
    fi

    configurar_db

    local mvnw="$BACKEND_DIR/mvnw"

    if [[ ! -f "$mvnw" ]]; then
        log "ERROR: no se encontró el wrapper Maven en $BACKEND_DIR/mvnw."
        return 1
    fi

    # En Git Bash, mvnw puede no tener permiso ejecutable.
    chmod +x "$mvnw" 2>/dev/null || true

    log "Iniciando la API Spring Boot en segundo plano (log: $LOG_FILE)..."

    (
        cd "$BACKEND_DIR"

        if command -v setsid >/dev/null 2>&1; then
            log "Usando setsid para aislar el backend."

            setsid nohup ./mvnw spring-boot:run \
                </dev/null >"$LOG_FILE" 2>&1 9>&- &

        else
            log "setsid no está disponible; usando nohup como alternativa."

            nohup ./mvnw spring-boot:run \
                </dev/null >"$LOG_FILE" 2>&1 9>&- &
        fi

        echo $! >"$PID_FILE"
    )

    local intento

    for intento in $(seq 1 120); do
        if backend_arriba; then
            log "API lista en http://localhost:8080."
            return
        fi

        sleep 1
    done

    log "ERROR: la API no respondió en 120 segundos."
    log "Últimas líneas del log:"

    tail -n 40 "$LOG_FILE" 2>/dev/null || true

    return 1
}

configurar_adb_reverse() {
    local adb_bin=""

    # Primero intenta encontrar adb en PATH.
    adb_bin="$(command -v adb 2>/dev/null || true)"

    # Busca en las ubicaciones habituales del Android SDK.
    if [[ -z "$adb_bin" ]]; then
        local sdk
        local candidato

        for sdk in \
            "${ANDROID_HOME:-}" \
            "${ANDROID_SDK_ROOT:-}" \
            "${LOCALAPPDATA:-}/Android/Sdk" \
            "$HOME/Android/Sdk" \
            "$HOME/AppData/Local/Android/Sdk"; do

            [[ -z "$sdk" ]] && continue

            sdk="$(convertir_ruta "$sdk")"

            for candidato in \
                "$sdk/platform-tools/adb" \
                "$sdk/platform-tools/adb.exe"; do

                if [[ -x "$candidato" ]]; then
                    adb_bin="$candidato"
                    break 2
                fi
            done
        done
    fi

    if [[ -z "$adb_bin" || ! -x "$adb_bin" ]]; then
        log "AVISO: adb no está disponible."
        log "Configurá manualmente: adb reverse tcp:8080 tcp:8080"
        return
    fi

    local dispositivos

    dispositivos="$("$adb_bin" devices 9>&- |
        awk 'NR > 1 && $2 == "device" { print $1 }')"

    if [[ -z "$dispositivos" ]]; then
        log "AVISO: no hay dispositivo/emulador conectado."
        log "No se configuró adb reverse."
        return
    fi

    local serial

    while IFS= read -r serial; do
        [[ -z "$serial" ]] && continue

        if "$adb_bin" -s "$serial" reverse tcp:8080 tcp:8080 \
            9>&- >/dev/null 2>&1; then

            log "adb reverse listo en $serial."
        else
            log "AVISO: no se pudo configurar adb reverse en $serial."
        fi

    done <<<"$dispositivos"
}

# Ejecución principal.
cargar_firebase_service_account
iniciar_api
configurar_adb_reverse

log "Preparación del entorno finalizada."
