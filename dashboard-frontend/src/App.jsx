import { useState, useEffect, useCallback, useMemo } from "react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  
} from "recharts";

const API = process.env.REACT_APP_ANALYTICS_URL || "http://localhost:8085";

const colors = {
  ink: "#102a43",
  subInk: "#486581",
  pageTop: "#f5f7f2",
  pageBottom: "#eef4ff",
  card: "#ffffff",
  border: "#d9e2ec",
  success: "#137547",
  danger: "#c81e1e",
  v1: "#1565c0",
  v2: "#f57c00",
  lavender: "#dde7ff",
  mint: "#dff9ec",
};

function usePoll(url, interval = 5000, enabled = true, refreshToken = 0) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!enabled) {
      return;
    }
    try {
      setLoading(true);
      const res = await fetch(url);
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const json = await res.json();
      setData(json);
      setError(null);
      setLastUpdated(new Date());
    } catch (e) {
      setError(e.message || "Network error");
    } finally {
      setLoading(false);
    }
  }, [enabled, url]);

  useEffect(() => {
    if (!enabled) {
      return;
    }
    fetchData();
    const id = setInterval(fetchData, interval);
    return () => clearInterval(id);
  }, [enabled, fetchData, interval, refreshToken]);

  return { data, error, lastUpdated, loading };
}

function Section({ title, subtitle, children }) {
  return (
    <section
      style={{
        background: colors.card,
        border: `1px solid ${colors.border}`,
        borderRadius: 18,
        padding: 18,
      }}
    >
      <div style={{ marginBottom: 14 }}>
        <h2 style={{ margin: 0, color: colors.ink, fontSize: 18 }}>{title}</h2>
        {subtitle ? (
          <p style={{ margin: "6px 0 0", color: colors.subInk, fontSize: 13 }}>{subtitle}</p>
        ) : null}
      </div>
      {children}
    </section>
  );
}

function Metric({ label, value, help, tone = "neutral" }) {
  const accent = tone === "bad" ? colors.danger : tone === "good" ? colors.success : colors.v1;
  return (
    <div
      style={{
        border: `1px solid ${colors.border}`,
        borderLeft: `5px solid ${accent}`,
        borderRadius: 12,
        padding: 12,
        background: "#fff",
      }}
    >
      <div style={{ color: colors.subInk, fontSize: 12, textTransform: "uppercase", letterSpacing: "0.04em" }}>
        {label}
      </div>
      <div style={{ color: colors.ink, fontSize: 28, lineHeight: 1.1, marginTop: 4, fontWeight: 700 }}>
        {value}
      </div>
      {help ? <div style={{ color: colors.subInk, fontSize: 12, marginTop: 6 }}>{help}</div> : null}
    </div>
  );
}

function FlowStep({ index, title, text }) {
  return (
    <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
      <div
        style={{
          width: 28,
          height: 28,
          flexShrink: 0,
          borderRadius: "50%",
          background: colors.lavender,
          border: `1px solid ${colors.border}`,
          display: "grid",
          placeItems: "center",
          fontWeight: 700,
          color: colors.v1,
          marginTop: 2,
        }}
      >
        {index}
      </div>
      <div>
        <div style={{ color: colors.ink, fontWeight: 700 }}>{title}</div>
        <div style={{ color: colors.subInk, marginTop: 3, fontSize: 13, lineHeight: 1.45 }}>{text}</div>
      </div>
    </div>
  );
}

function formatHour(isoValue) {
  if (!isoValue) {
    return "";
  }
  const d = new Date(isoValue);
  return `${String(d.getHours()).padStart(2, "0")}:00`;
}

function parseDiffSummary(raw) {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function toCsvRow(values) {
  return values
    .map((value) => {
      const normalized = String(value ?? "");
      if (normalized.includes(",") || normalized.includes('"') || normalized.includes("\n")) {
        return `"${normalized.replace(/"/g, '""')}"`;
      }
      return normalized;
    })
    .join(",");
}

function ConnectionPill({ ok, text }) {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 8,
        borderRadius: 999,
        padding: "6px 12px",
        fontSize: 12,
        border: `1px solid ${ok ? "#8dd8b5" : "#f2a6a6"}`,
        color: ok ? colors.success : colors.danger,
        background: ok ? colors.mint : "#ffe9e9",
        fontWeight: 700,
      }}
    >
      <span>{ok ? "Connected" : "Disconnected"}</span>
      <span style={{ color: colors.subInk, fontWeight: 500 }}>{text}</span>
    </span>
  );
}

export default function App() {
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [refreshEverySec, setRefreshEverySec] = useState(6);
  const [refreshToken, setRefreshToken] = useState(0);
  const [mismatchSearch, setMismatchSearch] = useState("");
  const [mismatchMethod, setMismatchMethod] = useState("all");

  const pollMs = refreshEverySec * 1000;
  const forceRefresh = useCallback(() => {
    setRefreshToken((n) => n + 1);
  }, []);

  const summaryPoll = usePoll(`${API}/api/analytics/summary`, pollMs, autoRefresh, refreshToken);
  const latencyPoll = usePoll(`${API}/api/analytics/latency`, pollMs, autoRefresh, refreshToken);
  const mismatchesPoll = usePoll(`${API}/api/analytics/mismatches?limit=50`, pollMs, autoRefresh, refreshToken);
  const endpointsPoll = usePoll(`${API}/api/analytics/endpoints`, pollMs, autoRefresh, refreshToken);
  const timelinePoll = usePoll(`${API}/api/analytics/timeline`, pollMs, autoRefresh, refreshToken);

  const summary = summaryPoll.data;
  const latency = Array.isArray(latencyPoll.data) ? latencyPoll.data : [];
  const mismatches = Array.isArray(mismatchesPoll.data) ? mismatchesPoll.data : [];
  const endpoints = Array.isArray(endpointsPoll.data) ? endpointsPoll.data : [];
  const timeline = Array.isArray(timelinePoll.data) ? timelinePoll.data : [];

  const anyLoading = [summaryPoll, latencyPoll, mismatchesPoll, endpointsPoll, timelinePoll].some((poll) => poll.loading);
  const errors = [summaryPoll, latencyPoll, mismatchesPoll, endpointsPoll, timelinePoll]
    .map((poll) => poll.error)
    .filter(Boolean);
  const uniqueMethods = useMemo(
    () => Array.from(new Set(mismatches.map((row) => (row.method || "UNKNOWN").toUpperCase()))).sort(),
    [mismatches]
  );

  const filteredMismatches = useMemo(() => {
    return mismatches.filter((row) => {
      const method = (row.method || "UNKNOWN").toUpperCase();
      const path = (row.path || "").toLowerCase();
      const query = mismatchSearch.trim().toLowerCase();
      const byMethod = mismatchMethod === "all" || method === mismatchMethod;
      const bySearch = !query || method.includes(query) || path.includes(query);
      return byMethod && bySearch;
    });
  }, [mismatchMethod, mismatchSearch, mismatches]);

  const exportEndpointsCsv = useCallback(() => {
    if (!endpoints.length) {
      return;
    }
    const header = toCsvRow(["Path", "Mismatch Rate %", "Mismatched Requests", "Total Requests"]);
    const rows = endpoints.map((row) =>
      toCsvRow([
        row.path || "unknown",
        Number(row.mismatchRatePct ?? 0).toFixed(2),
        row.mismatchedRequests ?? 0,
        row.totalRequests ?? 0,
      ])
    );
    const csv = [header, ...rows].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `endpoint-mismatch-${new Date().toISOString().replace(/[:.]/g, "-")}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [endpoints]);

  const backendConnected = useMemo(() => {
    return [summaryPoll, latencyPoll, mismatchesPoll, endpointsPoll, timelinePoll].some(
      (poll) => !!poll.data && !poll.error
    );
  }, [summaryPoll, latencyPoll, mismatchesPoll, endpointsPoll, timelinePoll]);

  const lastUpdated = useMemo(() => {
    const all = [
      summaryPoll.lastUpdated,
      latencyPoll.lastUpdated,
      mismatchesPoll.lastUpdated,
      endpointsPoll.lastUpdated,
      timelinePoll.lastUpdated,
    ].filter(Boolean);
    if (all.length === 0) {
      return null;
    }
    return all.sort((a, b) => b.getTime() - a.getTime())[0];
  }, [summaryPoll, latencyPoll, mismatchesPoll, endpointsPoll, timelinePoll]);

  const total = summary?.totalRequests ?? 0;
  const mismatchRate = summary?.mismatchRatePct ?? 0;
  const avgDelta = summary?.avgLatencyDeltaMs ?? 0;

  return (
    <div
      style={{
        minHeight: "100vh",
        padding: "20px",
        background: `linear-gradient(180deg, ${colors.pageTop} 0%, ${colors.pageBottom} 100%)`,
        fontFamily: "'IBM Plex Sans', 'Segoe UI', sans-serif",
      }}
    >
      <div style={{ maxWidth: 1300, margin: "0 auto" }}>
        <header
          style={{
            background: colors.card,
            border: `1px solid ${colors.border}`,
            borderRadius: 20,
            padding: "18px 20px",
            marginBottom: 16,
            boxShadow: "0 5px 16px rgba(16, 42, 67, 0.07)",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
            <div>
              <h1 style={{ margin: 0, fontSize: 29, color: colors.ink }}>Shadow Traffic Control Center</h1>
              <p style={{ margin: "8px 0 0", color: colors.subInk, fontSize: 14, maxWidth: 820 }}>
                This page explains your backend in plain language and shows live status. Send traffic, then watch
                how much v2 differs from v1.
              </p>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <ConnectionPill
                ok={backendConnected}
                text={backendConnected ? `${API} is reachable` : `${API} not responding`}
              />
            </div>
          </div>
          <div style={{ marginTop: 10, color: colors.subInk, fontSize: 12 }}>
            Last update: {lastUpdated ? lastUpdated.toLocaleString() : "Waiting for data"}
          </div>
          <div style={{ marginTop: 12, display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" }}>
            <button
              onClick={() => setAutoRefresh((v) => !v)}
              style={{
                border: `1px solid ${colors.border}`,
                borderRadius: 8,
                background: autoRefresh ? colors.mint : "#fff",
                color: colors.ink,
                fontWeight: 600,
                padding: "8px 12px",
                cursor: "pointer",
              }}
            >
              {autoRefresh ? "Auto-refresh: ON" : "Auto-refresh: OFF"}
            </button>
            <label style={{ display: "flex", gap: 6, alignItems: "center", color: colors.subInk, fontSize: 13 }}>
              Refresh interval
              <select
                value={refreshEverySec}
                onChange={(e) => setRefreshEverySec(Number(e.target.value))}
                style={{ border: `1px solid ${colors.border}`, borderRadius: 8, padding: "6px 8px", color: colors.ink }}
              >
                <option value={3}>3s</option>
                <option value={6}>6s</option>
                <option value={10}>10s</option>
                <option value={15}>15s</option>
              </select>
            </label>
            <button
              onClick={forceRefresh}
              style={{
                border: `1px solid ${colors.v1}`,
                borderRadius: 8,
                background: "#fff",
                color: colors.v1,
                fontWeight: 700,
                padding: "8px 12px",
                cursor: "pointer",
              }}
            >
              Refresh now
            </button>
            <span style={{ color: colors.subInk, fontSize: 12 }}>{anyLoading ? "Fetching latest data..." : "Data up to date"}</span>
          </div>
          {errors.length > 0 ? (
            <div
              style={{
                marginTop: 12,
                padding: "10px 12px",
                borderRadius: 10,
                background: "#ffe9e9",
                border: "1px solid #f2a6a6",
                color: colors.danger,
                fontSize: 12,
              }}
            >
              API errors: {Array.from(new Set(errors)).join(" | ")}
            </div>
          ) : null}
        </header>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 12, marginBottom: 16 }}>
          <Metric label="Requests Seen" value={total.toLocaleString()} help="Total mirrored requests processed" />
          <Metric
            label="Mismatch Rate"
            value={`${mismatchRate.toFixed(1)}%`}
            tone={mismatchRate > 5 ? "bad" : "good"}
            help="How often v2 output differs from v1"
          />
          <Metric
            label="v1 vs v2 Avg Latency Gap"
            value={`${Math.round(avgDelta)}ms`}
            tone={avgDelta > 80 ? "bad" : "neutral"}
            help="Positive means v2 is slower"
          />
          <Metric
            label="Current Mismatch Count"
            value={(summary?.mismatchedRequests ?? 0).toLocaleString()}
            tone={(summary?.mismatchedRequests ?? 0) > 0 ? "bad" : "good"}
            help="Absolute number of detected differences"
          />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1.1fr 1fr", gap: 14, marginBottom: 14 }}>
          <Section
            title="What Is Happening In Your System"
            subtitle="Use this to understand the platform flow before looking at charts"
          >
            <div style={{ display: "grid", gap: 10 }}>
              <FlowStep index="1" title="Client hits API Gateway" text="Gateway receives request and forwards it safely." />
              <FlowStep index="2" title="Traffic Duplicator mirrors request" text="v1 responds to user, v2 runs as shadow call." />
              <FlowStep index="3" title="Comparator checks both outputs" text="Differences and latency deltas are computed and saved." />
              <FlowStep index="4" title="Analytics exposes metrics" text="This dashboard reads summary, timeline, endpoint, and mismatch APIs." />
            </div>
          </Section>

          <Section title="Quick How-To" subtitle="When numbers are not changing, follow these checks">
            <ul style={{ margin: 0, paddingLeft: 18, color: colors.subInk, lineHeight: 1.6, fontSize: 13 }}>
              <li>Start stack: docker compose up --build -d</li>
              <li>Generate traffic: py load_test.py --rps 20 --duration 120</li>
              <li>Open this dashboard and check Requests Seen increases</li>
              <li>If Disconnected, verify analytics service on port 8085</li>
              <li>Try chaos mode to force differences and see mismatch rise</li>
            </ul>
          </Section>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14, marginBottom: 14 }}>
          <Section title="Latency Trend" subtitle="Blue is stable v1, orange is experimental v2">
            <ResponsiveContainer width="100%" height={250}>
              <LineChart
                data={latency.map((row) => ({
                  hour: formatHour(row.hour),
                  avgV1Ms: row.avgV1Ms,
                  avgV2Ms: row.avgV2Ms,
                }))}
              >
                <CartesianGrid strokeDasharray="3 3" stroke={colors.border} />
                <XAxis dataKey="hour" tick={{ fill: colors.subInk, fontSize: 11 }} />
                <YAxis tick={{ fill: colors.subInk, fontSize: 11 }} unit="ms" />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="avgV1Ms" name="v1 latency" stroke={colors.v1} strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="avgV2Ms" name="v2 latency" stroke={colors.v2} strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </Section>

          <Section title="Match vs Mismatch Timeline" subtitle="Green should dominate; red spikes indicate regression windows">
            <ResponsiveContainer width="100%" height={250}>
              <AreaChart
                data={timeline.map((row) => ({
                  hour: formatHour(row.hour),
                  matched: row.matched,
                  mismatched: row.mismatched,
                }))}
              >
                <CartesianGrid strokeDasharray="3 3" stroke={colors.border} />
                <XAxis dataKey="hour" tick={{ fill: colors.subInk, fontSize: 11 }} />
                <YAxis tick={{ fill: colors.subInk, fontSize: 11 }} />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="matched" stroke={colors.success} fill="#1db95433" strokeWidth={2} />
                <Area type="monotone" dataKey="mismatched" stroke={colors.danger} fill="#f4433630" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </Section>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1fr", gap: 14 }}>
          <Section title="Recent Mismatches" subtitle="If empty, your v1 and v2 are currently aligned">
            <div style={{ display: "flex", gap: 8, marginBottom: 10, flexWrap: "wrap" }}>
              <input
                value={mismatchSearch}
                onChange={(e) => setMismatchSearch(e.target.value)}
                placeholder="Search path or method"
                style={{
                  minWidth: 220,
                  border: `1px solid ${colors.border}`,
                  borderRadius: 8,
                  padding: "8px 10px",
                  color: colors.ink,
                }}
              />
              <select
                value={mismatchMethod}
                onChange={(e) => setMismatchMethod(e.target.value)}
                style={{
                  border: `1px solid ${colors.border}`,
                  borderRadius: 8,
                  padding: "8px 10px",
                  color: colors.ink,
                }}
              >
                <option value="all">All methods</option>
                {uniqueMethods.map((method) => (
                  <option key={method} value={method}>
                    {method}
                  </option>
                ))}
              </select>
              <div style={{ color: colors.subInk, fontSize: 12, display: "flex", alignItems: "center" }}>
                Showing {filteredMismatches.length} of {mismatches.length}
              </div>
            </div>
            {filteredMismatches.length === 0 ? (
              <div style={{ color: colors.subInk, padding: "12px 0" }}>No mismatches yet.</div>
            ) : (
              <div style={{ display: "grid", gap: 8, maxHeight: 320, overflowY: "auto" }}>
                {filteredMismatches.map((row, i) => {
                  const diffs = parseDiffSummary(row.diffSummary);
                  return (
                    <details
                      key={i}
                      style={{
                        border: `1px solid ${colors.border}`,
                        borderRadius: 10,
                        padding: "8px 10px",
                        background: "#fff",
                      }}
                    >
                      <summary style={{ cursor: "pointer", color: colors.ink, fontSize: 13 }}>
                        {row.method} {row.path} | v1 {row.v1Status} vs v2 {row.v2Status} | delta {row.v2LatencyMs - row.v1LatencyMs}ms
                      </summary>
                      <div style={{ marginTop: 8, color: colors.subInk, fontSize: 12 }}>
                        {diffs.length === 0 ? "No diff payload details" : `${diffs.length} field difference(s)`}
                        {diffs.slice(0, 5).map((d, idx) => (
                          <div key={idx} style={{ marginTop: 6, fontFamily: "'IBM Plex Mono', monospace" }}>
                            {d.path}: v1={String(d.v1Value)} | v2={String(d.v2Value)} [{d.type}]
                          </div>
                        ))}
                      </div>
                    </details>
                  );
                })}
              </div>
            )}
          </Section>

          <Section title="Endpoints With Highest Mismatch" subtitle="Shows where your experimental version is diverging most">
            <div style={{ marginBottom: 10 }}>
              <button
                onClick={exportEndpointsCsv}
                disabled={!endpoints.length}
                style={{
                  border: `1px solid ${colors.border}`,
                  borderRadius: 8,
                  background: endpoints.length ? "#fff" : "#f7f7f7",
                  color: endpoints.length ? colors.ink : colors.subInk,
                  fontWeight: 600,
                  padding: "7px 10px",
                  cursor: endpoints.length ? "pointer" : "not-allowed",
                }}
              >
                Export endpoint report (CSV)
              </button>
            </div>
            <ResponsiveContainer width="100%" height={320}>
              <BarChart
                layout="vertical"
                data={endpoints.slice(0, 7).map((row) => ({
                  path: (row.path || "").replace("/api/v1", "") || "unknown",
                  mismatchRate: Number((row.mismatchRatePct ?? 0).toFixed(1)),
                }))}
              >
                <CartesianGrid strokeDasharray="3 3" stroke={colors.border} horizontal={false} />
                <XAxis type="number" domain={[0, 100]} tick={{ fill: colors.subInk, fontSize: 11 }} unit="%" />
                <YAxis type="category" dataKey="path" width={120} tick={{ fill: colors.subInk, fontSize: 11 }} />
                <Tooltip formatter={(v) => [`${v}%`, "Mismatch"]} />
                <Bar dataKey="mismatchRate" fill="#ec7063" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Section>
        </div>
      </div>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500&display=swap');
        * {
          box-sizing: border-box;
        }
      `}</style>
    </div>
  );
}
