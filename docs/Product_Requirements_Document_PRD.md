# Product Requirements Document (PRD): AegisFinOps
**Product Owner:** [Your Name]  
**Status:** v1.0 — Production Prototype  
**North Star Metric:** Gross Margin Restored ($) & Blended SaaS Gross Margin (%)

## 1. Executive Summary & Problem Statement
B2B SaaS enterprises are rapidly embedding Generative AI features into their products. However, unlike traditional SaaS code where marginal cost per click is near $0, every LLM API call and GPU pod consumes variable token and compute COGS. Because most SaaS products charge flat monthly seat prices while paying variable token costs, "whale" customers silently erode product gross margins from 80% down to 45–55%.

**AegisFinOps** is an AI-native unit-economics gateway and analytics platform that attributes Kubernetes compute + LLM token spend to individual B2B tenants and features in real time, routes low-complexity queries to lower-cost models, blocks cost regressions in CI/CD, and generates executive P&L recovery recommendations.

## 2. Market Opportunity (TAM / SAM / SOM)
| Market Segment | Calculation Basis | Estimated Annual Size |
| :--- | :--- | :--- |
| **TAM (Total Addressable Market)** | 120,000 global cloud-native & AI-enabled enterprises spending an average of $150,000/yr on FinOps & observability tooling | **$18.0 Billion** |
| **SAM (Serviceable Available Market)** | 28,000 mid-market and enterprise B2B SaaS companies running Kubernetes + production LLM workloads | **$4.2 Billion** |
| **SOM (3-Year Target Share)** | Capturing 1.5% of SAM (420 enterprise customers at $35,000 ACV) | **$14.7 Million ARR** |

## 3. Target User Personas & Jobs-To-Be-Done (JTBD)
1. **VP of Product / Group PM:** Needs real-time visibility into gross margin per feature (`doc_qa`, `code_review`, `chat_support`) to set usage-based pricing tiers and sunset unprofitable features.
2. **Head of FinOps / VP Finance:** Needs automated budget guardrails to prevent flat-rate customers from consuming more in AI tokens than their monthly recurring revenue (MRR).
3. **Platform SRE / Engineering Lead:** Needs an automated model router (<15ms latency overhead) and a Jenkins CI/CD cost gate that stops developers from shipping bloated system prompts.

## 4. Mathematical Unit-Economics Model
* **Per-Request Total COGS ($):**
  `Request_COGS = (Input_Tokens * Model_Input_Price) + (Output_Tokens * Model_Output_Price) + (Duration_Sec * K8s_Pod_CPU_RAM_Cost_Per_Sec)`
* **Tenant Monthly Gross Margin (%):**
  `Gross_Margin_% = ((Tenant_MRR - Sum(Request_COGS)) / Tenant_MRR) * 100`
* **Counterfactual Routing Savings ($):**
  `Savings_USD = Requested_Frontier_Model_Cost - Routed_Small_Model_Cost`

## 5. RICE Feature Prioritization Matrix
| Feature | Reach (Tenants/Mo) | Impact (1-3) | Confidence (%) | Effort (Weeks) | RICE Score | Priority |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Smart Complexity Model Router (Java)** | 50 | 3 (Massive) | 95% | 2 | **71.3** | **P0** |
| **ClickHouse Real-Time Margin Engine** | 50 | 3 (Massive) | 95% | 2 | **71.3** | **P0** |
| **Tenant Budget Circuit Breaker & Upsell API** | 50 | 3 (Massive) | 90% | 2 | **67.5** | **P0** |
| **Jenkins CI/CD Cost-Regression Gate** | 50 | 2 (High) | 85% | 1.5 | **56.7** | **P1** |
| **GenAI Autonomous FinOps Advisor** | 50 | 2 (High) | 80% | 2 | **40.0** | **P1** |