package com.aegisfinops.gateway.observability;

import com.aegisfinops.gateway.router.SmartModelRoutingEngine.RoutingDecision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

@Service
public class ClickHouseAndPrometheusService {

    private final MeterRegistry meterRegistry;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${aegis.clickhouse.url:http://localhost:8123/}")
    private String clickHouseUrl;

    public ClickHouseAndPrometheusService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordTelemetry(
            String tenantId,
            String planTier,
            double monthlyMrrUsd,
            String featureName,
            int inputTokens,
            int sysPromptTokens,
            int outputTokens,
            RoutingDecision decision) {

        // 1. Emit Real-Time Prometheus Metrics
        Counter.builder("aegis_llm_cost_usd_total")
                .tag("tenant", tenantId)
                .tag("feature", featureName)
                .tag("routed_model", decision.routedModel())
                .register(meterRegistry)
                .increment(decision.totalCogsUsd());

        Counter.builder("aegis_llm_savings_usd_total")
                .tag("tenant", tenantId)
                .tag("feature", featureName)
                .tag("reason", decision.routingReason())
                .register(meterRegistry)
                .increment(decision.counterfactualSavingsUsd());

        // 2. Write Event to ClickHouse OLAP Table
        String insertSql = String.format(Locale.US,
            "INSERT INTO finops.llm_telemetry_events " +
            "(tenant_id, plan_tier, monthly_mrr_usd, feature_name, requested_model, routed_model, " +
            "routing_reason, complexity_score, input_tokens, system_prompt_tokens, output_tokens, " +
            "llm_token_cost_usd, k8s_pod_compute_cost_usd, total_cogs_usd, counterfactual_savings_usd, " +
            "latency_ms, cache_hit, http_status) VALUES " +
            "('%s', '%s', %.2f, '%s', '%s', '%s', '%s', %.2f, %d, %d, %d, %.6f, %.6f, %.6f, %.6f, %d, 0, 200)",
            tenantId, planTier, monthlyMrrUsd, featureName,
            decision.requestedModel(), decision.routedModel(), decision.routingReason(),
            decision.complexityScore(), inputTokens, sysPromptTokens, outputTokens,
            decision.actualLlmCostUsd(), decision.k8sPodCostUsd(),
            decision.totalCogsUsd(), decision.counterfactualSavingsUsd(), decision.simulatedLatencyMs()
        );
        executeClickHouseQuery(insertSql);
    }

    public String queryCustomerGrossMarginScorecard() {
        String sql = """
            SELECT
                c.tenant_id,
                c.company_name,
                c.plan_tier,
                c.monthly_mrr_usd,
                round(sum(e.total_cogs_usd), 2) AS total_cogs_usd,
                round(sum(e.counterfactual_savings_usd), 2) AS router_savings_usd,
                round(((c.monthly_mrr_usd - sum(e.total_cogs_usd)) / c.monthly_mrr_usd) * 100, 2) AS gross_margin_pct
            FROM finops.tenant_contracts AS c
            LEFT JOIN finops.llm_telemetry_events AS e ON c.tenant_id = e.tenant_id
            GROUP BY c.tenant_id, c.company_name, c.plan_tier, c.monthly_mrr_usd
            ORDER BY gross_margin_pct ASC
            FORMAT JSONEachRow
            """;
        return executeClickHouseQuery(sql);
    }

    public String queryFeatureUnitEconomics() {
        String sql = """
            SELECT
                feature_name,
                count() AS total_requests,
                round(sum(total_cogs_usd), 2) AS total_cogs_usd,
                round(sum(counterfactual_savings_usd), 2) AS router_savings_usd,
                round(avg(system_prompt_tokens), 0) AS avg_sys_prompt_tokens,
                round((sum(system_prompt_tokens) / sum(input_tokens)) * 100, 1) AS sys_prompt_bloat_pct
            FROM finops.llm_telemetry_events
            GROUP BY feature_name
            ORDER BY total_cogs_usd DESC
            FORMAT JSONEachRow
            """;
        return executeClickHouseQuery(sql);
    }

    public String executeClickHouseQuery(String sql) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clickHouseUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(sql))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}