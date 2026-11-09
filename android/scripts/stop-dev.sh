#!/usr/bin/env bash
#
# Detiene la API local que inició `prepare-dev.sh`.
# No afecta a un backend que hayas arrancado manualmente en otra terminal.

set -euo pipefail

PID_FILE="${SHOPPI_BACKEND_PID:-/tmp/shoppi-backend.pid}"

if [[ ! -f "$PID_FILE" ]]; then
    echo "[shoppi-dev] No hay una API iniciada por prepare-dev.sh."
    exit 0
fi

pid="$(cat "$PID_FILE")"
if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "[shoppi-dev] No hay una API iniciada por prepare-dev.sh."
    exit 0
fi

# Recolecta el proceso y sus descendientes antes de matar al padre.
pids="$pid $(pgrep -P "$pid" 2>/dev/null | tr '\n' ' ' || true)"

pgid="$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ' || true)"
if [[ -n "$pgid" && "$pgid" == "$pid" ]]; then
    # La API se inició con setsid: es líder de su propio grupo.
    kill -TERM -- "-$pgid" 2>/dev/null || true
else
    # Compatibilidad con arranques anteriores: primero los hijos.
    for p in $pids; do
        kill -TERM "$p" 2>/dev/null || true
    done
fi

for _ in $(seq 1 20); do
    vivo=0
    for p in $pids; do
        kill -0 "$p" 2>/dev/null && vivo=1
    done
    [[ "$vivo" == "0" ]] && break
    sleep 0.5
done

for p in $pids; do
    kill -KILL "$p" 2>/dev/null || true
done

rm -f "$PID_FILE"
echo "[shoppi-dev] API detenida (PID $pid)."
