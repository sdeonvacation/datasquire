#!/usr/bin/env bash
#
# DataSquire Finance Controller Demo Script
# Runs through enterprise finance queries demonstrating key capabilities.
# Requires: the app running on port 8080 (demo or live mode).
#
set -euo pipefail

BASE_URL="http://localhost:8080/api"
SESSION_ID="cfo-session-$(date +%s)"

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

header() {
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}  $1${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

query() {
  local description="$1"
  local question="$2"

  echo ""
  echo -e "${YELLOW}>> $description${NC}"
  echo "   Question: \"$question\""
  echo ""
  curl -sN "$BASE_URL/query" \
    -H "Content-Type: application/json" \
    -d "{\"query\": \"$question\", \"sessionId\": \"$SESSION_ID\"}"
  echo ""
  sleep 1
}

# ─────────────────────────────────────────────────────────────────────────────
header "1. LIST AVAILABLE AGENTS"
# Shows which sub-agents are registered for SQL generation
echo ""
echo -e "${YELLOW}>> Listing registered agents${NC}"
curl -s "$BASE_URL/agents" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/agents"
sleep 1

# ─────────────────────────────────────────────────────────────────────────────
header "2. REVENUE AGGREGATION"
# Demonstrates: RAG retrieval of revenue-related schema chunks,
# multi-table JOIN (journal_lines + chart_of_accounts + business_units),
# GROUP BY with SUM aggregation
query "Revenue by business unit (RAG + aggregation)" \
  "What is our total revenue YTD by business unit?"

# ─────────────────────────────────────────────────────────────────────────────
header "3. BUDGET VS ACTUAL VARIANCE"
# Demonstrates: complex comparison query joining budgets table to actuals,
# HAVING clause to filter overspend, computed variance columns
query "Budget variance analysis (joins + computed columns)" \
  "Which business units are over budget for Q3?"

# ─────────────────────────────────────────────────────────────────────────────
header "4. FILTERED QUERY WITH BUSINESS IMPACT"
# Demonstrates: WHERE clause with multiple conditions,
# calculated columns (days_overdue), ORDER BY on amount
query "Overdue invoices (filter + calculated fields)" \
  "Show me all overdue invoices over \$10,000"

# ─────────────────────────────────────────────────────────────────────────────
header "5. TIME SERIES ANALYSIS"
# Demonstrates: DATE_TRUNC for time grouping, cash flow categorization,
# monthly trend generation
query "Cash flow trend (time series + grouping)" \
  "What is our operating cash flow trend by month?"

# ─────────────────────────────────────────────────────────────────────────────
header "6. FOLLOW-UP QUERY (SESSION MEMORY)"
# Uses the same sessionId — demonstrates conversation continuity.
# DataSquire remembers prior questions about Engineering overspend.
query "Follow-up with context (session memory)" \
  "How does Engineering's spending compare to their budget?"

# ─────────────────────────────────────────────────────────────────────────────
header "7. VENDOR SPEND ANALYSIS"
# Demonstrates: GROUP BY with multiple aggregates (COUNT, SUM, conditional SUM),
# LIMIT for top-N ranking
query "Top vendors by spend (aggregation + ranking)" \
  "Which vendors have the highest total spend?"

# ─────────────────────────────────────────────────────────────────────────────
header "DEMO COMPLETE"
echo ""
echo "  Session ID: $SESSION_ID"
echo "  All queries used the same session, demonstrating conversation continuity."
echo ""
echo "  Try your own queries:"
echo "    curl -sN $BASE_URL/query -H 'Content-Type: application/json' \\"
echo "      -d '{\"query\": \"What are our pending journal entries?\", \"sessionId\": \"$SESSION_ID\"}'"
echo ""
