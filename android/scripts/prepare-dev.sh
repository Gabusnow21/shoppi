#!/usr/bin/env bash
#
# Prepara el entorno local antes de compilar/instalar la app desde Android
# Studio:
#
#   1. Levanta la API Spring Boot en segundo plano si todavía no responde.
#   2. Configura `adb reverse tcp:8080 tcp:8080` en cada dispositivo conectado,
#      para que el teléfono/emulador pueda usar http://127.0.0.1:8080/api/.
#
# Es idempotente: si la API ya está arriba no la vuelve a iniciar. Se ejecuta
# automáticamente desde la tarea Gradle `prepareDevEnvironment`, que a su vez es
# dependencia de `assembleDebug`/`installDebug`.
#
# Variables opcionales:
#   SHOPPI_SKIP_DEV_BACKEND=1  no hace nada (útil en CI).
#   DB_URL, DB_USER, DB_PASS   conexión a PostgreSQL (si no, se detecta 5433/5432).
#   SHOPPI_BACKEND_LOG         archivo de log (default /tmp/shoppi-backend.log).
#   SHOPPI_BACKEND_PID         archivo de PID (default /tmp/shoppi-backend.pid).

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

if [[ "${SHOPPI_SKIP_DEV_BACKEND:-0}" == "1" ]]; then
    log "SHOPPI_SKIP_DEV_BACKEND=1: no se inicia la API."
    exit 0
fi

# Evita dos arranques simultáneos si Gradle lanza builds en paralelo.
exec 9>"${TMPDIR:-/tmp}/shoppi-dev-backend.lock"
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
        return
    fi

    local valor
    valor="$(sed -n 's/^shoppi[.]firebaseServiceAccount=//p' "$local_props" | tail -n 1)"
    if [[ -z "$valor" ]]; then
        log "ERROR: falta shoppi.firebaseServiceAccount en $local_props."
        return 1
    fi
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

    if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
        log "ERROR: no se encontró el wrapper Maven en $BACKEND_DIR/mvnw."
        return 1
    fi

    log "Iniciando la API Spring Boot en segundo plano (log: $LOG_FILE)..."
    (
        cd "$BACKEND_DIR"
        # setsid aísla la API en su propio grupo de procesos para poder
        # detenerla luego sin tocar a Gradle/Android Studio. 9>&- evita que el
        # proceso de larga vida herede el lock de arranque; si lo heredara, el
        # siguiente `prepareDevEnvironment` quedaría colgado.
        setsid nohup ./mvnw spring-boot:run </dev/null >"$LOG_FILE" 2>&1 9>&- &
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

    log "ERROR: la API no respondió en 120 s. Últimas líneas del log:"
    tail -n 40 "$LOG_FILE" 2>/dev/null || true
    return 1
}

configurar_adb_reverse() {
    local adb_bin
    adb_bin="$(command -v adb 2>/dev/null || true)"

    if [[ -z "$adb_bin" ]]; then
        local candidato
        for candidato in \
            "${ANDROID_HOME:-}/platform-tools/adb" \
            "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
            "$HOME/Android/Sdk/platform-tools/adb"; do
            if [[ -x "$candidato" ]]; then
                adb_bin="$candidato"
                break
            fi
        done
    fi

    if [[ -z "$adb_bin" || ! -x "$adb_bin" ]]; then
        log "AVISO: adb no está disponible; configurá 'adb reverse tcp:8080 tcp:8080' a mano."
        return
    fi

    local dispositivos
    dispositivos="$("$adb_bin" devices | awk 'NR > 1 && $2 == "device" { print $1 }')"

    if [[ -z "$dispositivos" ]]; then
        log "AVISO: no hay dispositivo/emulador conectado; no se configuró adb reverse."
        return
    fi

    local serial
    while IFS= read -r serial; do
        [[ -z "$serial" ]] && continue
        if "$adb_bin" -s "$serial" reverse tcp:8080 tcp:8080 >/dev/null 2>&1; then
            log "adb reverse listo en $serial (dispositivo:8080 -> PC:8080)."
        else
            log "AVISO: no se pudo configurar adb reverse en $serial."
        fi
    done <<<"$dispositivos"
}

cargar_firebase_service_account

iniciar_api
configurar_adb_reverse
