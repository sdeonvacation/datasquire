# Finance Controller Dashboard — Demo Guide

This guide walks through DataSquire's capabilities using the finance controller scenario.
A CFO or controller asks natural language questions about financial data; DataSquire
translates them into SQL, executes against PostgreSQL, and returns formatted answers.

## Setup

```bash
# Start database with finance schema
docker compose up -d

# Set your OpenAI API key
export OPENAI_API_KEY=sk-...

# Ingest schema reference into vector store
java -jar datasquire-ingest.jar \
  --schema-file=src/main/resources/schema/finance-schema-reference.md \
  --namespace=finance

# Start the app
./mvnw -pl datasquire-example-app spring-boot:run
```

Or run in demo mode (no API key):

```bash
docker compose up -d
./mvnw -pl datasquire-example-app spring-boot:run -Dspring-boot.run.profiles=demo
./demo/run-demo.sh
```

---

## Scenario 1: Revenue by Business Unit (Simple Aggregation)

**Input:**
```bash
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is our total revenue YTD by business unit?", "sessionId": "cfo-session"}'
```

**What DataSquire Does:**
1. **RAG retrieval** — embeds query, cosine-searches pgvector, retrieves schema chunks for `journal_lines`, `chart_of_accounts`, `business_units`
2. **SQL generation** — LLM generates: sum credit_amount on REVENUE accounts, grouped by BU
3. **Execution** — runs against PostgreSQL with statement timeout + row limits
4. **Synthesis** — LLM formats results as a ranked table with dollar amounts

**Output (SSE events):**
```
event: progress
data: {"status":"resolving_session"}

event: progress
data: {"status":"orchestrating","sessionId":"cfo-session"}

event: data
data: {"response":"**Query:** `SELECT bu.name, SUM(jl.credit_amount) AS total_revenue FROM journal_lines jl JOIN journal_entries je ON ... WHERE coa.account_type = 'REVENUE' GROUP BY bu.name ORDER BY total_revenue DESC;`\n\n| business_unit | total_revenue |\n|---|---|\n| Sales | $25,140,000 |\n| Engineering | $12,080,000 |\n| Operations | $8,760,000 |\n\nSales leads revenue at **$25.1M** YTD, driven by product and consulting streams."}

event: done
data: {"sessionId":"cfo-session","iterations":1,"agentsUsed":["text-to-sql"],"qualityScore":0.95}
```

---

## Scenario 2: Budget vs Actual (Variance Analysis)

**Input:**
```bash
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Which business units are over budget for Q3?", "sessionId": "cfo-session"}'
```

**What DataSquire Does:**
1. **RAG retrieval** — retrieves chunks for `budgets`, `journal_lines`, `journal_entries`, `business_units`
2. **SQL generation** — LLM generates a comparison: actual spend (journal_lines debit where Jul-Sep) vs budget q3_amount, filtered by variance > 0
3. **Execution** — multi-table JOIN with HAVING clause
4. **Synthesis** — formats as variance table highlighting overspend

**Output:**
```
event: data
data: {"response":"**Query:** `SELECT bu.name, coa.account_name, SUM(jl.debit_amount) AS actual, b.q3_amount AS budget, SUM(jl.debit_amount) - b.q3_amount AS variance FROM ...`\n\n| business_unit | account | actual | budget | variance |\n|---|---|---|---|---|\n| Engineering | Cloud Infrastructure | $1,425,000 | $900,000 | +$525,000 |\n| Engineering | Professional Services | $387,000 | $250,000 | +$137,000 |\n\n⚠️ **Engineering exceeded Q3 budget** by $662K total, primarily driven by cloud scaling events and contractor costs during the platform rebuild."}
```

---

## Scenario 3: Overdue Invoices (Filtered Query with Impact)

**Input:**
```bash
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Show me all overdue invoices over $10,000", "sessionId": "cfo-session"}'
```

**What DataSquire Does:**
1. **RAG retrieval** — retrieves chunks for `invoices`, `business_units`
2. **SQL generation** — WHERE status='OVERDUE' AND amount > 10000, computes days_overdue
3. **Execution** — filtered query with calculated column
4. **Synthesis** — formats with aging and risk assessment

**Output:**
```
event: data
data: {"response":"**Query:** `SELECT i.vendor_name, i.amount, i.due_date, CURRENT_DATE - i.due_date AS days_overdue, bu.name FROM invoices i JOIN business_units bu ON ... WHERE i.status = 'OVERDUE' AND i.amount > 10000 ORDER BY i.amount DESC;`\n\n| vendor | amount | due_date | days_overdue | business_unit |\n|---|---|---|---|---|\n| Accenture | $450,000 | 2024-09-01 | 121 | Sales |\n| McKinsey & Co | $325,000 | 2024-09-14 | 108 | Sales |\n| AWS | $185,000 | 2024-10-19 | 73 | Engineering |\n| ... |\n\n**12 overdue invoices** totaling **$1.58M**. Sales has the highest exposure ($890K across 5 invoices)."}
```

---

## Scenario 4: Cash Flow Trend (Time Series)

**Input:**
```bash
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is our operating cash flow trend by month?", "sessionId": "cfo-session"}'
```

**What DataSquire Does:**
1. **RAG retrieval** — retrieves chunk for `cash_flow` table
2. **SQL generation** — DATE_TRUNC by month, SUM amounts, filter OPERATING
3. **Execution** — grouped time series
4. **Synthesis** — formats monthly trend showing net position

**Output:**
```
event: data
data: {"response":"**Query:** `SELECT DATE_TRUNC('month', flow_date) AS month, SUM(amount) AS net_operating FROM cash_flow WHERE flow_type = 'OPERATING' GROUP BY month ORDER BY month;`\n\n| month | net_operating |\n|---|---|\n| Jan 2024 | -$560,000 |\n| Feb 2024 | $565,000 |\n| Mar 2024 | -$1,990,000 |\n| ... | ... |\n| Dec 2024 | $2,250,000 |\n\nOperating cash flow was **negative in Q1** (seasonal payroll timing) but recovered strongly in Q4. Full-year net operating: **+$4.2M**."}
```

---

## Scenario 5: Follow-Up Question (Session Memory)

Using the same `sessionId` from the previous queries — DataSquire remembers context.

**Input:**
```bash
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "How does Engineering'\''s spending compare to their budget?", "sessionId": "cfo-session"}'
```

**What DataSquire Does:**
1. **Session load** — retrieves prior conversation (knows we discussed Engineering Q3 overspend)
2. **RAG retrieval** — retrieves `budgets`, `journal_lines`, `business_units` chunks
3. **SQL generation** — full-year actual vs budget for Engineering BU, all expense accounts
4. **Synthesis** — contextualizes with prior Q3 discussion

**Output:**
```
event: data
data: {"response":"**Query:** `SELECT coa.account_name, SUM(jl.debit_amount) AS actual_ytd, b.annual_total AS budget, ... WHERE bu.name = 'Engineering' AND coa.account_type = 'EXPENSE' ...`\n\n| account | actual_ytd | annual_budget | variance | % |\n|---|---|---|---|---|\n| Salaries and Wages | $12,240,000 | $12,240,000 | $0 | 0% |\n| Cloud Infrastructure | $4,850,000 | $3,500,000 | +$1,350,000 | +39% |\n| Professional Services | $1,180,000 | $900,000 | +$280,000 | +31% |\n| Software Licenses | $650,000 | $650,000 | $0 | 0% |\n\nAs flagged in our Q3 discussion, **Cloud Infrastructure is 39% over budget** ($1.35M). The Q3 platform rebuild drove most of this overage."}
```

---

## Internal Architecture Flow

```
User Question (natural language)
     │
     ▼
[Session Store] ── loads conversation history
     │
     ▼
[Orchestrator] ── routes to appropriate sub-agent
     │
     ▼
[Schema RAG] ── embeds query → cosine search pgvector → top-8 schema chunks
     │
     ▼
[Text-to-SQL Agent] ── LLM generates SQL from schema + question + history
     │
     ▼
[SQL Safety Layer] ── validates: read-only, no DDL/DML
     │
     ▼
[SQL Executor] ── runs query with statement timeout + row limits
     │
     ▼
[Response Synthesizer] ── LLM formats results as business insight
     │
     ▼
SSE Stream to Client
```

## Endpoints Reference

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/query | Submit question (SSE streaming response) |
| GET | /api/agents | List registered sub-agents |
| GET | /api/sessions/{id} | Retrieve session state |
| DELETE | /api/sessions/{id} | Delete session |
