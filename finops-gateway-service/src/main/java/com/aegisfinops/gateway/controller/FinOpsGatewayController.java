package com.aegisfinops.gateway.controller;

import com.aegisfinops.gateway.genai.AutonomousFinOpsAdvisor;
import com.aegisfinops.gateway.observability.ClickHouseAndPrometheusService;
import com.aegisfinops.gateway.router.SmartModelRoutingEngine;
import com.aegisfinops.gateway.router.SmartModelRoutingEngine.RoutingDecision;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FinOpsGatewayController {

    private final SmartModelRoutingEngine routingEngine;
    private final ClickHouseAndPrometheusService telemetryService;
    private final AutonomousFinOpsAdvisor aiAdvisor;

    public FinOpsGatewayController(
            SmartModelRoutingEngine routingEngine,
            ClickHouseAndPrometheusService telemetryService,
            AutonomousFinOpsAdvisor aiAdvisor) {
        this.routingEngine = routingEngine;
        this.telemetryService = telemetryService;
        this.aiAdvisor = aiAdvisor;
    }

    public record LlmCompletionRequest(
        String tenantId,
        String planTier,
        Double monthlyMrrUsd,
        String featureName,
        String requestedModel,
        String prompt,
        Integer inputTokens,
        Integer systemPromptTokens,
        Integer outputTokens,
        Double currentTenantMarginPct
    ) {}

    @PostMapping("/llm/completions")
    public Map<String, Object> processLlmRequest(@RequestBody LlmCompletionRequest req) {
        String tenant = req.tenantId() != null ? req.tenantId() : "tenant_acme_legal";
        String plan = req.planTier() != null ? req.planTier() : "Growth_Flat";
        double mrr = req.monthlyMrrUsd() != null ? req.monthlyMrrUsd() : 1500.0;
        String feature = req.featureName() != null ? req.featureName() : "chat_support";
        String model = req.requestedModel() != null ? req.requestedModel() : "gpt-4o";
        String prompt = req.prompt() != null ? req.prompt() : "Summarize user ticket status";
        int inTokens = req.inputTokens() != null ? req.inputTokens() : 2500;
        int sysTokens = req.systemPromptTokens() != null ? req.systemPromptTokens() : 450;
        int outTokens = req.outputTokens() != null ? req.outputTokens() : 400;
        double currentMargin = req.currentTenantMarginPct() != null ? req.currentTenantMarginPct() : 72.0;

        RoutingDecision decision = routingEngine.evaluateAndRoute(
                tenant, feature, model, prompt, inTokens, outTokens, currentMargin
        );

        telemetryService.recordTelemetry(
                tenant, plan, mrr, feature, inTokens, sysTokens, outTokens, decision
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", tenant);
        response.put("featureName", feature);
        response.put("routingDecision", decision);
        response.put("simulatedAiResponse", "Processed by AegisFinOps via model: " + decision.routedModel());
        return response;
    }

    @GetMapping("/finops/advisor/executive-memo")
    public Map<String, Object> getExecutiveMemo() {
        return aiAdvisor.generateExecutiveMarginRecoveryMemo();
    }
}