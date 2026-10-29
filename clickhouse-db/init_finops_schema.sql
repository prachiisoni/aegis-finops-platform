CREATE DATABASE IF NOT EXISTS finops;

-- 1. Enterprise Tenant Subscription Contracts Table (Revenue Side of P&L)
CREATE TABLE IF NOT EXISTS finops.tenant_contracts (
    tenant_id String,
    company_name String,
    plan_tier LowCardinality(String),
    monthly_mrr_usd Float64,
    target_gross_margin_pct Float64,
    account_owner String,
    created_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree()
ORDER BY (tenant_id);

-- 2. Raw LLM & Kubernetes Compute Telemetry Table (COGS Side of P&L)
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
ORDER BY (tenant_id, feature_name, event_time);

-- 3. Real-Time Hourly Unit-Economics Materialized View
CREATE MATERIALIZED VIEW IF NOT EXISTS finops.hourly_feature_economics_mv
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(hour_bucket)
ORDER BY (tenant_id, feature_name, routed_model, hour_bucket)
AS SELECT
    toStartOfHour(event_time) AS hour_bucket,
    tenant_id,
    plan_tier,
    feature_name,
    requested_model,
    routed_model,
    count() AS total_requests,
    sum(input_tokens) AS sum_input_tokens,
    sum(system_prompt_tokens) AS sum_system_prompt_tokens,
    sum(output_tokens) AS sum_output_tokens,
    sum(llm_token_cost_usd) AS sum_llm_cost_usd,
    sum(k8s_pod_compute_cost_usd) AS sum_k8s_cost_usd,
    sum(total_cogs_usd) AS sum_total_cogs_usd,
    sum(counterfactual_savings_usd) AS sum_savings_usd
FROM finops.llm_telemetry_events
GROUP BY hour_bucket, tenant_id, plan_tier, feature_name, requested_model, routed_model;

-- 4. Seed 10 B2B SaaS Enterprise Customers
INSERT INTO finops.tenant_contracts (tenant_id, company_name, plan_tier, monthly_mrr_usd, target_gross_margin_pct, account_owner) VALUES
('tenant_acme_legal', 'AcmeLegal Corp', 'Growth_Flat', 1500.00, 75.0, 'Priya Sharma'),
('tenant_globex_fin', 'Globex Financial', 'Enterprise', 8500.00, 80.0, 'Rahul Verma'),
('tenant_initech_ai', 'Initech Cloud', 'Starter', 500.00, 70.0, 'Ananya Patel'),
('tenant_stark_med', 'Stark MedTech', 'Enterprise', 12000.00, 80.0, 'Vikram Singh'),
('tenant_wayne_retail', 'Wayne Retail Group', 'Growth_Flat', 2000.00, 75.0, 'Priya Sharma'),
('tenant_umbrella_bio', 'Umbrella BioLabs', 'Enterprise', 9500.00, 80.0, 'Rahul Verma'),
('tenant_cyberdyne_sys', 'Cyberdyne Systems', 'Growth_Flat', 1800.00, 75.0, 'Ananya Patel'),
('tenant_soylent_ops', 'Soylent Supply Chain', 'Starter', 600.00, 70.0, 'Vikram Singh'),
('tenant_hooli_search', 'Hooli Enterprise', 'Enterprise', 15000.00, 82.0, 'Priya Sharma'),
('tenant_piedpiper_dev', 'PiedPiper Compression', 'Growth_Flat', 2200.00, 75.0, 'Rahul Verma');