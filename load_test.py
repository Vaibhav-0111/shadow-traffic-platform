#!/usr/bin/env python3
"""
Shadow Traffic Load Generator
===============================
Fires realistic order API requests at the gateway so you can
watch the dashboard light up with live comparison data.

Usage:
    python3 load_test.py                  # 10 req/s default
    python3 load_test.py --rps 50         # 50 req/s
    python3 load_test.py --chaos          # enable v2 chaos
    python3 load_test.py --rps 20 --duration 120
"""

import argparse
import json
import random
import time
import uuid
import requests
import threading
from datetime import datetime

GATEWAY = "http://localhost:8080"
PRODUCTS = ["prod-001", "prod-002", "prod-003", "prod-042", "prod-099"]
CUSTOMERS = [f"cust-{i:04d}" for i in range(1, 51)]

stats = {"sent": 0, "ok": 0, "err": 0}
lock = threading.Lock()


def random_order():
    items = [
        {
            "productId":   random.choice(PRODUCTS),
            "productName": f"Product {random.randint(1,9)}",
            "quantity":    random.randint(1, 5),
            "unitPrice":   round(random.uniform(9.99, 199.99), 2),
        }
        for _ in range(random.randint(1, 4))
    ]
    total = sum(i["quantity"] * i["unitPrice"] for i in items)
    return {
        "customerId":  random.choice(CUSTOMERS),
        "totalAmount": round(total, 2),
        "currency":    "USD",
        "items":       items,
    }


def fire_request():
    # Mix of create / get / list
    roll = random.random()
    try:
        if roll < 0.6:
            # POST create order
            r = requests.post(f"{GATEWAY}/api/v1/orders",
                              json=random_order(),
                              headers={"Content-Type": "application/json"},
                              timeout=5)
        elif roll < 0.85:
            # GET single order
            oid = random.randint(1, max(1, stats["ok"]))
            r = requests.get(f"{GATEWAY}/api/v1/orders/{oid}", timeout=5)
        else:
            # GET list
            r = requests.get(f"{GATEWAY}/api/v1/orders?page=0&size=10", timeout=5)

        with lock:
            stats["sent"] += 1
            if r.status_code < 500:
                stats["ok"] += 1
            else:
                stats["err"] += 1

    except Exception as e:
        with lock:
            stats["sent"] += 1
            stats["err"] += 1


def printer():
    while True:
        time.sleep(5)
        with lock:
            s = stats.copy()
        print(f"[{datetime.now().strftime('%H:%M:%S')}] "
              f"sent={s['sent']:,}  ok={s['ok']:,}  err={s['err']:,}  "
              f"err_rate={0 if s['sent']==0 else s['err']/s['sent']*100:.1f}%")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rps",      type=int,   default=10)
    parser.add_argument("--duration", type=int,   default=300)
    parser.add_argument("--chaos",    action="store_true")
    args = parser.parse_args()

    if args.chaos:
        print("⚠️  Enabling chaos on v2: 200ms delay + 15% failure rate")
        print("   Set env vars on order-service-v2 container:")
        print("   CHAOS_DELAY_MS=200  CHAOS_FAIL_RATE=0.15")
        print()

    print(f"🚀 Starting load: {args.rps} req/s for {args.duration}s → {GATEWAY}")
    print(f"   Dashboard: http://localhost:3000")
    print()

    threading.Thread(target=printer, daemon=True).start()

    interval = 1.0 / args.rps
    end_time = time.time() + args.duration

    while time.time() < end_time:
        t = threading.Thread(target=fire_request, daemon=True)
        t.start()
        time.sleep(interval)

    print("\n✅ Load test complete.")
    with lock:
        print(f"   Total: {stats}")


if __name__ == "__main__":
    main()
