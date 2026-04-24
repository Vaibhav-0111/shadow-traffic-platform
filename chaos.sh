#!/bin/bash
# chaos.sh — Runtime chaos injection without rebuilding containers
# Uses Docker to set env vars on the running order-service-v2 container.
# ─────────────────────────────────────────────────────────────────────────────

set -e

CONTAINER="shadow-traffic-platform-order-service-v2-1"

print_usage() {
  echo ""
  echo "Usage: ./chaos.sh [scenario]"
  echo ""
  echo "Scenarios:"
  echo "  slow     — 300ms artificial delay (latency regression)"
  echo "  veryslow — 1000ms delay (extreme latency)"
  echo "  flaky    — 20% random failure rate"
  echo "  death    — 60% failure rate (nearly dead service)"
  echo "  combo    — 200ms delay + 15% failures (realistic)"
  echo "  reset    — remove all chaos (back to normal)"
  echo ""
}

apply() {
  local delay=$1
  local fail=$2
  echo "🔥 Applying chaos: delay=${delay}ms  fail_rate=${fail}"
  docker exec "$CONTAINER" kill -SIGUSR2 1 2>/dev/null || true
  # Update env vars via docker exec (requires Spring Boot Actuator env refresh)
  curl -s -X POST "http://localhost:8083/actuator/env" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"chaos.delay-ms\",\"value\":\"${delay}\"}" > /dev/null
  curl -s -X POST "http://localhost:8083/actuator/env" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"chaos.fail-rate\",\"value\":\"${fail}\"}" > /dev/null
  curl -s -X POST "http://localhost:8083/actuator/refresh" > /dev/null
  echo "✅ Chaos applied — watch the dashboard!"
  echo "   Dashboard: http://localhost:3000"
}

case "$1" in
  slow)     apply 300 0.0  ;;
  veryslow) apply 1000 0.0 ;;
  flaky)    apply 0 0.2    ;;
  death)    apply 0 0.6    ;;
  combo)    apply 200 0.15 ;;
  reset)    apply 0 0.0 && echo "🟢 Chaos cleared — v2 is healthy again" ;;
  *)        print_usage    ;;
esac
