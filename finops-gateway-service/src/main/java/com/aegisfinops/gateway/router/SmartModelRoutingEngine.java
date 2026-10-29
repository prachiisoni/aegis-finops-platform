package com.aegisfinops.gateway.router;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SmartModelRoutingEngine {

    // Price per 1 Million tokens: [Input_USD, Output_USD]
    private static final Map<String, double[]> MODEL_PRICING = Map.of(
        "gpt-4o", new double[]{5.00, 15.00},
        "claude-3-5-sonnet", new double[]{3.00, 15.00},
        "gpt-4o-mini", new double[]{0.15, 0.60},
        "llama-3-1-8b-instruct", new double[]{0.15, 0.60}
    );

    public record RoutingDecision(
        String requestedModel,
        String routedModel,
        String routingReason,
        double complexityScore,
        double actualLlmCostUsd,
        double k8sPodCostUsd,
        double totalCogsUsd,
        double counterfactualSavingsUsd,
        int simulatedLatencyMs,
        String budgetStatus
    ) {}

    public double scorePromptComplexity(String prompt, String featureName) {
        double score = 0.15;
        int length = prompt != null ? prompt.length() : 0;
        if (length > 2000) score += 0.35;
        else if (length > 600) score += 0.15;

        String lower = prompt != null ? prompt.toLowerCase() : "";
        if (lower.contains("analyze") || lower.contains("synthesize") || lower.contains("contract") || lower.contains("sql")) {
            score += 0.30;
        }
        if ("executive_report".equalsIgnoreCase(featureName)) {
            score += 0.25;
        }
        return Math.min(1.0, Math.round(score * 100.0) / 100.0);
    }

    public RoutingDecision evaluateAndRoute(
            String tenantId,
            String featureName,
            String requestedModel,
            String prompt,
            int inputTokens,
            int outputTokens,
            double currentTenantMarginPct) {

        String modelReq = MODEL_PRICING.containsKey(requestedModel) ? requestedModel : "gpt-4o";
        double complexity = scorePromptComplexity(prompt, featureName);

        String routedModel = modelReq;
        String reason = "DIRECT_PASS_THROUGH";
        String budgetStatus = "HEALTHY";

        // 1. Tenant Budget Circuit Breaker: Margin Protection Mode if tenant margin < 55%
        if (currentTenantMarginPct < 0.0) {
            budgetStatus = "CRITICAL_NEGATIVE_MARGIN_UPSELL_REQUIRED";
            routedModel = "gpt-4o-mini";
            reason = "CIRCUIT_BREAKER_MARGIN_PROTECTION";
        } else if (currentTenantMarginPct < 55.0) {
            budgetStatus = "AT_RISK_MARGIN_GUARD_ACTIVE";
            if (!"executive_report".equalsIgnoreCase(featureName)) {
                routedModel = "gpt-4o-mini";
                reason = "MARGIN_FLOOR_DOWNGRADE";
            }
        } else if (complexity < 0.45 && !"executive_report".equalsIgnoreCase(featureName)) {
            // 2. Standard Semantic Complexity Routing
            routedModel = "gpt-4o".equals(modelReq) ? "gpt-4o-mini" : "llama-3-1-8b-instruct";
            reason = "SMART_COMPLEXITY_DOWNGRADE";
        }

        double[] reqPrices = MODEL_PRICING.get(modelReq);
        double[] routePrices = MODEL_PRICING.get(routedModel);

        double baselineCost = (inputTokens / 1_000_000.0) * reqPrices[0] + (outputTokens / 1_000_000.0) * reqPrices[1];
        double actualLlmCost = (inputTokens / 1_000_000.0) * routePrices[0] + (outputTokens / 1_000_000.0) * routePrices[1];
        double savings = Math.max(0.0, baselineCost - actualLlmCost);

        int latencyMs = routedModel.equals(modelReq) ? 1150 : 310;
        double k8sCost = (latencyMs / 1000.0) * 0.00018;
        double totalCogs = actualLlmCost + k8sCost;

        return new RoutingDecision(
            modelReq, routedModel, reason, complexity,
            actualLlmCost, k8sCost, totalCogs, savings, latencyMs, budgetStatus
        );
    }
}