import random
import urllib.error
import urllib.request
from datetime import datetime, timedelta

CLICKHOUSE_URL = "http://localhost:8123/"

TENANTS = [
    ("tenant_acme_legal", "AcmeLegal Corp", "Growth_Flat", 1500.00, 75.0, "Priya Sharma"),
    ("tenant_globex_fin", "Globex Financial", "Enterprise", 8500.00, 80.0, "Rahul Verma"),
    ("tenant_initech_ai", "Initech Cloud", "Starter", 500.00, 70.0, "Ananya Patel"),
    ("tenant_stark_med", "Stark MedTech", "Enterprise", 12000.00, 80.0, "Vikram Singh"),
    ("tenant_wayne_retail", "Wayne Retail Group", "Growth_Flat", 2000.00, 75.0, "Priya Sharma"),
    ("tenant_umbrella_bio", "Umbrella BioLabs", "Enterprise", 9500.00, 80.0, "Rahul Verma"),
    ("tenant_cyberdyne_sys", "Cyberdyne Systems", "Growth_Flat", 1800.00, 75.0, "Ananya Patel"),
    ("tenant_soylent_ops", "Soylent Supply Chain", "Starter", 600.00, 70.0, "Vikram Singh"),
    ("tenant_hooli_search", "Hooli Enterprise", "Enterprise", 15000.00, 82.0, "Priya Sharma"),
    ("tenant_piedpiper_dev", "PiedPiper Compression", "Growth_Flat", 2200.00, 75.0, "Rahul Verma"),
]

FEATURES = ["doc_qa", "code_review", "chat_support", "executive_report"]

MODEL_PRICING = {
    "gpt-4o": (5.00, 15.00),
    "claude-3-5-sonnet": (3.00, 15.00),
    "llama-3-1-8b-instruct": (0.15, 0.60),
    "gpt-4o-mini": (0.15, 0.60),
}

def execute_sql(sql_payload: str) -> str:
    req = urllib.request.Request(CLICKHOUSE_URL, data=sql_payload.encode("utf-8"), method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"ClickHouse HTTP {e.code}: {err_msg}") from e

def initialize_schema():
    print("1. Initializing ClickHouse 'finops' database and tables...")
    execute_sql("CREATE DATABASE IF NOT EXISTS finops")

    execute_sql("""
    CREATE TABLE IF NOT EXISTS finops.tenant_contracts (
        tenant_id String,
        company_name String,
        plan_tier LowCardinality(String),
        monthly_mrr_usd Float64,
        target_gross_margin_pct Float64,
        account_owner String,
        created_at DateTime DEFAULT now()
    ) ENGINE = ReplacingMergeTree()
    ORDER BY (tenant_id)
    """)

    execute_sql("""
    CREATE TABLE IF NOT EXISTS finops.llm_telemetry_events (
        event_id UUID DEFAULT generateUUIDv4(),
        event_time DateTime DEFAULT now(),
        tenant_id LowCardinality(String),
        plan_tier LowCardinality(String),
        monthly_mrr_usd Float64,
        feature_name LowCardinality(String),
        requested_model LowCardinality(String),
        routed_model LowCardinality(String),
        routing_reason LowCardinality(String),
        complexity_score Float32,
        input_tokens UInt32,
        system_prompt_tokens UInt32,
        output_tokens UInt32,
        llm_token_cost_usd Float64,
        k8s_pod_compute_cost_usd Float64,
        total_cogs_usd Float64,
        counterfactual_savings_usd Float64,
        latency_ms UInt32,
        cache_hit UInt8,
        http_status UInt16
    ) ENGINE = MergeTree()
    PARTITION BY toYYYYMM(event_time)
    ORDER BY (tenant_id, feature_name, event_time)
    """)

    execute_sql("TRUNCATE TABLE finops.tenant_contracts")
    execute_sql("TRUNCATE TABLE finops.llm_telemetry_events")

    contract_values = ",".join(
        f"('{t_id}', '{name}', '{tier}', {mrr}, {target}, '{owner}')"
        for t_id, name, tier, mrr, target, owner in TENANTS
    )
    execute_sql(
        "INSERT INTO finops.tenant_contracts "
        "(tenant_id, company_name, plan_tier, monthly_mrr_usd, target_gross_margin_pct, account_owner) "
        "VALUES " + contract_values
    )

def generate_simulation():
    initialize_schema()
    print("2. Generating 15,000+ multi-tenant SaaS AI telemetry events across 30 days...")
    now = datetime.utcnow()
    rows = []

    for day_offset in range(30, 0, -1):
        event_day = now - timedelta(days=day_offset)
        smart_router_enabled = day_offset <= 15

        for _ in range(500):
            tenant_id, _, plan_tier, mrr, _, _ = random.choice(TENANTS)
            feature = random.choice(FEATURES)

            if tenant_id == "tenant_acme_legal" and random.random() < 0.75:
                feature = "doc_qa"
                input_tokens = random.randint(22000, 38000)
                sys_prompt_tokens = 1200
                output_tokens = random.randint(2000, 3500)
                requested_model = "gpt-4o"
                complexity = round(random.uniform(0.20, 0.85), 2)
            elif feature == "chat_support":
                input_tokens = random.randint(3500, 4800)
                sys_prompt_tokens = 3200
                output_tokens = random.randint(250, 600)
                requested_model = "gpt-4o"
                complexity = round(random.uniform(0.10, 0.40), 2)
            else:
                input_tokens = random.randint(800, 4000)
                sys_prompt_tokens = 450
                output_tokens = random.randint(300, 1200)
                requested_model = random.choice(["gpt-4o", "claude-3-5-sonnet"])
                complexity = round(random.uniform(0.10, 0.90), 2)

            if smart_router_enabled and complexity < 0.45 and feature != "executive_report":
                routed_model = "gpt-4o-mini" if requested_model == "gpt-4o" else "llama-3-1-8b-instruct"
                routing_reason = "SMART_COMPLEXITY_DOWNGRADE"
            else:
                routed_model = requested_model
                routing_reason = "DIRECT_PASS_THROUGH"

            req_in_price, req_out_price = MODEL_PRICING[requested_model]
            route_in_price, route_out_price = MODEL_PRICING[routed_model]

            baseline_llm_cost = (input_tokens / 1e6) * req_in_price + (output_tokens / 1e6) * req_out_price
            actual_llm_cost = (input_tokens / 1e6) * route_in_price + (output_tokens / 1e6) * route_out_price
            savings_usd = max(0.0, baseline_llm_cost - actual_llm_cost)

            latency_ms = random.randint(220, 650) if routed_model != requested_model else random.randint(850, 2400)
            k8s_cost = round((latency_ms / 1000.0) * 0.00018, 6)
            total_cogs = round(actual_llm_cost + k8s_cost, 6)

            ts_str = (event_day + timedelta(seconds=random.randint(0, 86000))).strftime("%Y-%m-%d %H:%M:%S")
            rows.append(
                f"('{ts_str}', '{tenant_id}', '{plan_tier}', {mrr}, '{feature}', "
                f"'{requested_model}', '{routed_model}', '{routing_reason}', {complexity}, "
                f"{input_tokens}, {sys_prompt_tokens}, {output_tokens}, "
                f"{actual_llm_cost:.6f}, {k8s_cost:.6f}, {total_cogs:.6f}, {savings_usd:.6f}, "
                f"{latency_ms}, {1 if random.random() < 0.15 else 0}, 200)"
            )

            if len(rows) >= 2500:
                sql = (
                    "INSERT INTO finops.llm_telemetry_events "
                    "(event_time, tenant_id, plan_tier, monthly_mrr_usd, feature_name, "
                    "requested_model, routed_model, routing_reason, complexity_score, "
                    "input_tokens, system_prompt_tokens, output_tokens, llm_token_cost_usd, "
                    "k8s_pod_compute_cost_usd, total_cogs_usd, counterfactual_savings_usd, "
                    "latency_ms, cache_hit, http_status) VALUES " + ",".join(rows)
                )
                execute_sql(sql)
                rows.clear()

    print("3. SUCCESS! Querying ClickHouse Live Customer Gross Margin Scorecard:\n")
    report = execute_sql("""
    SELECT
        c.company_name AS Company,
        c.plan_tier AS Plan,
        c.monthly_mrr_usd AS MRR_USD,
        round(sum(e.total_cogs_usd), 2) AS Total_COGS_USD,
        round(sum(e.counterfactual_savings_usd), 2) AS Router_Savings_USD,
        round(((c.monthly_mrr_usd - sum(e.total_cogs_usd)) / c.monthly_mrr_usd) * 100, 1) AS Gross_Margin_Pct
    FROM finops.tenant_contracts AS c
    LEFT JOIN finops.llm_telemetry_events AS e ON c.tenant_id = e.tenant_id
    GROUP BY c.company_name, c.plan_tier, c.monthly_mrr_usd
    ORDER BY Gross_Margin_Pct ASC
    FORMAT PrettyCompactMonoBlock
    """)
    print(report)

if __name__ == "__main__":
    generate_simulation()