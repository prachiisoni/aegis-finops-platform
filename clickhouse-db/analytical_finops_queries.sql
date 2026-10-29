-- QUERY 1: Live Customer Gross Margin & P&L Leaderboard (Finds "Whale" Margin Bleeders)
SELECT
    c.tenant_id,
    c.company_name,
    c.plan_tier,
    c.monthly_mrr_usd AS monthly_revenue_usd,
    round(sum(e.total_cogs_usd), 2) AS total_cogs_usd,
    round(sum(e.counterfactual_savings_usd), 2) AS smart_router_savings_usd,
    round(((c.monthly_mrr_usd - sum(e.total_cogs_usd)) / c.monthly_mrr_usd) * 100, 2) AS actual_gross_margin_pct,
    c.target_gross_margin_pct,
    multiIf(
        ((c.monthly_mrr_usd - sum(e.total_cogs_usd)) / c.monthly_mrr_usd) * 100 < 0, 'CRITICAL: NEGATIVE MARGIN (UPSELL OR THROTTLE)',
        ((c.monthly_mrr_usd - sum(e.total_cogs_usd)) / c.monthly_mrr_usd) * 100 < c.target_gross_margin_pct, 'AT RISK: SMART ROUTING ACTIVE',
        'HEALTHY'
    ) AS commercial_status
FROM finops.tenant_contracts AS c
LEFT JOIN finops.llm_telemetry_events AS e ON c.tenant_id = e.tenant_id
GROUP BY c.tenant_id, c.company_name, c.plan_tier, c.monthly_mrr_usd, c.target_gross_margin_pct
ORDER BY actual_gross_margin_pct ASC;

-- QUERY 2: Product Feature Unit-Economics Matrix (For Product Managers)
SELECT
    feature_name,
    count() AS total_calls,
    round(avg(latency_ms), 1) AS avg_latency_ms,
    round(sum(total_cogs_usd), 2) AS total_feature_cogs_usd,
    round((sum(total_cogs_usd) / count()) * 1000, 4) AS cost_per_1k_requests_usd,
    round(sum(counterfactual_savings_usd), 2) AS router_savings_captured_usd,
    round((countIf(routed_model != requested_model) / count()) * 100, 1) AS pct_calls_smart_routed
FROM finops.llm_telemetry_events
GROUP BY feature_name
ORDER BY total_feature_cogs_usd DESC;

-- QUERY 3: System Prompt Bloat Audit (Detects Engineering Prompt Regressions)
SELECT
    feature_name,
    round(avg(system_prompt_tokens), 0) AS avg_system_prompt_tokens,
    round(avg(input_tokens), 0) AS avg_total_input_tokens,
    round((sum(system_prompt_tokens) / sum(input_tokens)) * 100, 1) AS system_prompt_overhead_pct,
    round(sum(llm_token_cost_usd * (system_prompt_tokens / input_tokens)), 2) AS wasted_system_prompt_spend_usd
FROM finops.llm_telemetry_events
GROUP BY feature_name
ORDER BY system_prompt_overhead_pct DESC;