#!/bin/sh
set -eu

CONFIG_PATH="${CONFIG_FILE:-stashield.toml}"
case "${CONFIG_PATH}" in
  /*) ;;
  *) CONFIG_PATH="/home/container/${CONFIG_PATH}" ;;
esac

BACKEND_TARGET_HOST="${BACKEND_HOST:-AUTO}"
if [ "${BACKEND_TARGET_HOST}" = "AUTO" ]; then
  BACKEND_TARGET_HOST="${SERVER_IP:-127.0.0.1}"
fi

LISTEN_PORT="${SERVER_PORT:-25565}"
BACKEND_TARGET_PORT="${BACKEND_PORT:-25565}"
MAX_PACKET_BYTES_VALUE="${MAX_PACKET_BYTES:-2097152}"
MAX_CONNECTIONS_VALUE="${MAX_CONNECTIONS:-2048}"
RATE_LIMIT_PER_IP_VALUE="${RATE_LIMIT_PER_IP:-8}"
BURST_LIMIT_PER_IP_VALUE="${BURST_LIMIT_PER_IP:-24}"
BURST_WINDOW_MS_VALUE="${BURST_WINDOW_MS:-3000}"
BOT_SCORE_THRESHOLD_VALUE="${BOT_SCORE_THRESHOLD:-70}"
MAX_ACTIVE_LOGINS_PER_IP_VALUE="${MAX_ACTIVE_LOGINS_PER_IP:-3}"

mkdir -p "$(dirname "${CONFIG_PATH}")"

cat > "${CONFIG_PATH}" <<EOF
[server]
bind_host = "0.0.0.0"
bind_port = ${LISTEN_PORT}
backlog = 256
connect_timeout_ms = 5000
read_timeout_ms = 10000
max_packet_bytes = ${MAX_PACKET_BYTES_VALUE}
max_connections = ${MAX_CONNECTIONS_VALUE}

[backend]
host = "${BACKEND_TARGET_HOST}"
port = ${BACKEND_TARGET_PORT}

[protection]
enabled = true
rate_limit_per_ip = ${RATE_LIMIT_PER_IP_VALUE}
burst_limit_per_ip = ${BURST_LIMIT_PER_IP_VALUE}
burst_window_ms = ${BURST_WINDOW_MS_VALUE}
bot_score_threshold = ${BOT_SCORE_THRESHOLD_VALUE}
max_active_logins_per_ip = ${MAX_ACTIVE_LOGINS_PER_IP_VALUE}

[access]
whitelist_enabled = false
whitelist = []
blacklist = []

[metrics]
enabled = true
log_interval_seconds = 30
EOF

MEMORY_MB="${SERVER_MEMORY:-512}"

exec java -Xms128M -Xmx"${MEMORY_MB}"M -jar /opt/stashield/STAShield.jar --config "${CONFIG_PATH}"
