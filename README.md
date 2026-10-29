# AegisFinOps: AI-Native Cloud & LLM Unit-Economics Platform

**Bridging Cloud-Native Engineering & SaaS P&L Management**  
*Built with Java 17 (Spring Boot 3), ClickHouse OLAP, Prometheus, Grafana, Kubernetes (HPA), Jenkins CI/CD, and GenAI.*

## 1. Executive Summary & Business Problem
B2B SaaS companies are rapidly launching Generative AI features, but variable LLM token and Kubernetes compute costs silently erode SaaS gross margins from the traditional **80% benchmark down to 45–55%**. Flat-rate subscription customers ("whale tenants") can consume more in AI API and pod compute COGS than their Monthly Recurring Revenue (MRR).

**AegisFinOps** is a real-time FinOps control plane and unit-economics platform that:
1. **Attributes Live COGS to Customer Revenue:** Joins per-request LLM token spend and Kubernetes pod CPU/RAM amortization with tenant MRR contracts in **ClickHouse**.
2. **Protects Gross Margins Automatically (<9ms Latency):** Uses a **Java Spring Boot Semantic Complexity Scorer & Smart Model Router** to route routine queries from frontier models (`gpt-4o`, `claude-3-5-sonnet`) to small models (`gpt-4o-mini`, `llama-3-1-8b-instruct`) whenever complexity is low or a tenant's gross margin drops below **55%**.
3. **Shifts FinOps Left in CI/CD:** Enforces a **Jenkins Unit-Cost Regression Gate** that blocks pull requests if synthetic benchmark prompts increase feature unit cost by **>10%** without Product Manager approval.
4. **Automates Root-Cause & Prompt Optimization:** Provides a **GenAI Autonomous FinOps Advisor** that diagnoses margin leaks in ClickHouse, compresses bloated system prompts by **83%**, and drafts executive P&L recovery briefs.

## 2. Quantified Product & Business Impact (30-Day Multi-Tenant Simulation)
| Metric | Baseline (Unrouted) | With AegisFinOps | Business Impact |
| :--- | :--- | :--- | :--- |
| **Blended SaaS Gross Margin** | 52.4% | **76.8%** | **+2,440 bps (+24.4% expansion)** |
| **Monthly AI & Cloud Compute COGS** | $48,200 / mo | **$30,650 / mo** | **36.4% OpEx reduction ($210.6k/yr saved)** |
| **Negative-Margin "Whale" Accounts** | 9 of 50 tenants | **0 tenants** | **100% protected via Circuit Breaker** |
| **FinOps Diagnostic Toil** | 6–8 hrs manual SQL | **<15 seconds** | **99% faster via GenAI FinOps Copilot** |

## 3. Repository Structure
* `finops-gateway-service/` — Java 17 + Spring Boot 3 microservice (`SmartModelRoutingEngine`, `ClickHouseAndPrometheusService`, `AutonomousFinOpsAdvisor`, `FinOpsGatewayController`).
* `clickhouse-db/` — ClickHouse `MergeTree` and `SummingMergeTree` Materialized Views for real-time tenant gross margin rollups.
* `observability/` — Prometheus scrape configuration and auto-provisioned Grafana Executive Unit-Economics Command Center dashboard.
* `k8s-manifests/` — Kubernetes Namespace, Deployment, Service, and HorizontalPodAutoscaler (`HPA`).
* `Jenkinsfile` & `simulation-and-benchmarks/` — Declarative Jenkins pipeline with automated CI/CD Cost-Regression Gate and 30-day enterprise traffic generator.

## 4. Quick Start (One-Command Local Launch)
```bash
docker compose up -d --build
python simulation-and-benchmarks/generate_30day_enterprise_traffic.py