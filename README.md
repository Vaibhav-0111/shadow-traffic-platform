# Shadow Traffic Platform

> Production-style shadow testing platform to validate new service behavior safely, without impacting real users.

## What Is This?

This project compares two versions of the same API:

- v1: stable user-facing service
- v2: experimental shadow service

Every incoming request is mirrored to both versions. Users always get the v1 response, while v2 is validated in the background.

## Languages And Tools Used

| Icon | Layer | Technology |
|---|---|---|
| ☕ | Backend microservices | Java 17 + Spring Boot |
| ⚛️ | Frontend dashboard | React + Recharts |
| 🐍 | Load generation | Python |
| 🐳 | Orchestration | Docker + Docker Compose |
| 📨 | Event streaming | Apache Kafka |
| 🗄️ | Storage | PostgreSQL |
| ⚡ | Cache/rate limiting | Redis |
| 🔍 | Message inspection | Kafka UI |

## Architecture (2D View)

```text
┌─────────────┐
│   Client    │
└──────┬──────┘
  │
  ▼
┌────────────────────┐
│ API Gateway :8080  │
└──────┬─────────────┘
  │
  ▼
┌────────────────────┐       ┌────────────────────┐
│ Traffic Duplicator │──────▶│ Order Service v2   │ (shadow)
│      :8081         │       │       :8083        │
└──────┬─────────────┘       └────────────────────┘
  │
  ├────────────────────▶┌────────────────────┐
  │                     │ Order Service v1   │ (user response)
  │                     │       :8082        │
  │                     └────────────────────┘
  │
  ▼
┌────────────────────┐
│ Kafka topic pairs  │
└──────┬─────────────┘
  ▼
┌────────────────────┐
│ Comparator :8084   │
└──────┬─────────────┘
  ▼
┌────────────────────┐
│ Analytics :8085    │
└──────┬─────────────┘
  ▼
┌────────────────────┐
│ Dashboard :3000    │
└────────────────────┘
```

## Features

- 🔁 Real request shadowing (v1 vs v2)
- 🧠 Async diff comparison via Kafka
- 📊 Interactive dashboard with plain-language insights
- ⏱️ Latency trend and mismatch timeline
- 🧪 Chaos toggles for delay/failure simulation
- 📌 Endpoint-level mismatch ranking

## Quick Start (How To Open And Use)

### 1. Prerequisites

- Docker Desktop running
- Python 3.8+ (for load test)

### 2. Optional but recommended: create .env

Copy and customize:

```powershell
copy .env.example .env
```

or

```bash
cp .env.example .env
```

### 3. Start all services

```bash
docker compose up --build -d
```

### 4. Verify health

```powershell
docker compose ps
curl.exe -s http://localhost:8080/actuator/health
powershell -NoProfile -Command "Invoke-RestMethod -UseBasicParsing http://localhost:8085/api/analytics/summary | ConvertTo-Json -Depth 5"
```

### 5. Open UI

- Dashboard: http://localhost:3000
- Kafka UI: http://localhost:9090

### 6. Generate load and watch live changes

```bash
py load_test.py --rps 20 --duration 120
```

### 7. Inject chaos (optional)

```bash
./chaos.sh slow
./chaos.sh flaky
./chaos.sh combo
./chaos.sh reset
```

## .env: What To Add And Why

This repo now supports Docker Compose variable substitution.

Use .env to:

- keep credentials and ports in one place
- avoid editing docker-compose.yml repeatedly
- switch environments fast (local/demo/test)

Main variables in .env.example:

```env
SPRING_PROFILES_ACTIVE=docker
POSTGRES_USER=shadow
POSTGRES_PASSWORD=shadow123
POSTGRES_DB=shadowdb
POSTGRES_PORT=5432

API_GATEWAY_PORT=8080
TRAFFIC_DUPLICATOR_PORT=8081
ORDER_SERVICE_V1_PORT=8082
ORDER_SERVICE_V2_PORT=8083
COMPARATOR_SERVICE_PORT=8084
ANALYTICS_SERVICE_PORT=8085
DASHBOARD_PORT=3000

REACT_APP_ANALYTICS_URL=http://localhost:8085

CHAOS_DELAY_MS=0
CHAOS_FAIL_RATE=0
```

## Why This Error Happened

### Error 1: "The import java.util.stream.Collectors is never used"

Reason:
- The class imported Collectors (and PageRequest) but did not use them.

Fix done:
- Removed unused imports from AnalyticsService.

### Error 2: PowerShell curl issues

Reason:
- In PowerShell, curl maps to Invoke-WebRequest, so Linux flags like -s -o -w can fail.

Fix:
- Use curl.exe for curl-style flags, or use Invoke-RestMethod directly.

## Interactive 2D Console Animation Demo

Run this demo to visualize request flow in terminal.

```python
# save as flow_demo.py and run: py flow_demo.py
import os
import time

frames = [
    "Client --> [Gateway]    [Duplicator]    [v1] [v2]    [Kafka]    [Comparator]    [Analytics]    [Dashboard]",
    "Client -----> [Gateway] [Duplicator]    [v1] [v2]    [Kafka]    [Comparator]    [Analytics]    [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] [v1] [v2] [Kafka]    [Comparator]    [Analytics]    [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] -> [v1] [v2] [Kafka] [Comparator]    [Analytics]    [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] -> [v1] [v2] -----> [Kafka] [Comparator] [Analytics] [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] -> [v1] [v2] -----> [Kafka] -----> [Comparator] [Analytics] [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] -> [v1] [v2] -----> [Kafka] -----> [Comparator] -----> [Analytics] [Dashboard]",
    "Client -----> [Gateway] -----> [Duplicator] -> [v1] [v2] -----> [Kafka] -----> [Comparator] -----> [Analytics] -----> [Dashboard]",
]

for i in range(20):
    os.system("cls" if os.name == "nt" else "clear")
    print("Shadow Traffic 2D Console Animation")
    print("=" * 90)
    print(frames[i % len(frames)])
    print("=" * 90)
    time.sleep(0.25)
```

## APIs To Explore

| Service | URL |
|---|---|
| Gateway health | http://localhost:8080/actuator/health |
| Analytics summary | http://localhost:8085/api/analytics/summary |
| Analytics latency | http://localhost:8085/api/analytics/latency |
| Analytics mismatches | http://localhost:8085/api/analytics/mismatches?limit=10 |
| Analytics endpoints | http://localhost:8085/api/analytics/endpoints |
| Analytics timeline | http://localhost:8085/api/analytics/timeline |

## Project Structure

```text
shadow-traffic-platform/
├── .env.example
├── docker-compose.yml
├── load_test.py
├── chaos.sh
├── api-gateway/
├── traffic-duplicator/
├── order-service-v1/
├── order-service-v2/
├── comparator-service/
├── analytics-service/
└── dashboard-frontend/
```
