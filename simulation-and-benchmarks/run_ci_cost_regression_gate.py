import os
import sys

BASELINE_COST_PER_1K_CALLS_USD = 4.20
MAX_ALLOWED_REGRESSION_PCT = 10.0

def run_cost_gate():
    print("=== AEGIS FINOPS: JENKINS CI/CD UNIT-COST REGRESSION GATE ===")
    simulated_pr_system_prompt_tokens = int(os.getenv("PR_SYSTEM_PROMPT_TOKENS", "520"))
    baseline_system_prompt_tokens = 480

    # Calculate projected unit cost per 1,000 calls on candidate build
    token_delta_ratio = simulated_pr_system_prompt_tokens / baseline_system_prompt_tokens
    projected_cost_per_1k = round(BASELINE_COST_PER_1K_CALLS_USD * token_delta_ratio, 3)
    cost_increase_pct = round(((projected_cost_per_1k - BASELINE_COST_PER_1K_CALLS_USD) / BASELINE_COST_PER_1K_CALLS_USD) * 100, 2)

    print(f"Production Baseline Cost / 1k Calls : ${BASELINE_COST_PER_1K_CALLS_USD:.2f}")
    print(f"Candidate PR Cost / 1k Calls        : ${projected_cost_per_1k:.2f} ({cost_increase_pct:+.2f}%)")
    print(f"Max Allowed Threshold               : +{MAX_ALLOWED_REGRESSION_PCT:.1f}%")

    if cost_increase_pct > MAX_ALLOWED_REGRESSION_PCT and os.getenv("PM_COST_OVERRIDE") != "true":
        print("\n[BLOCKED BY FINOPS GATE] Candidate PR increases unit cost by more than 10%!")
        print("Action Required: Compress system prompt tokens or set PM_COST_OVERRIDE=true with Product Manager approval.")
        sys.exit(1)

    print("\n[PASSED] Unit economics within target gross margin guardrails.")
    sys.exit(0)

if __name__ == "__main__":
    run_cost_gate()