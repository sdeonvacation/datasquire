# DataSquire Example App — Finance Controller Dashboard

Natural language interface for enterprise financial data. A CFO or controller asks
questions in plain English; DataSquire translates to SQL via RAG-augmented LLM,
executes against PostgreSQL, and streams back formatted business insights.

**Stack:** Spring Boot 3.4.1 • Spring AI 1.0.0 • PostgreSQL 17 + pgvector • OpenAI GPT-4o + text-embedding-3-small

## Quick Start

### Prerequisites

- Docker (for PostgreSQL + pgvector)
- Java 21+
- OpenAI API key

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Creates the `financedb` database with 7 tables, 30 GL accounts, 200 journal entries,
80 invoices, and 50 cash flow records. Provisions a read-only `datasquire_reader` role.

> **Note:** If you previously ran a different schema, reset the volume:
> `docker compose down -v && docker compose up -d`

### 2. Set OpenAI API Key

```bash
export OPENAI_API_KEY=sk-...
```

### 3. Ingest Schema into Vector Store

```bash
java -jar datasquire-ingest.jar \
  --schema-file=src/main/resources/schema/finance-schema-reference.md \
  --namespace=finance
```

This embeds the schema reference document into pgvector for RAG retrieval.

### 4. Start the App

```bash
./mvnw -pl datasquire-example-app spring-boot:run
```

### 5. Ask Questions

```bash
# Revenue analysis
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What is our total revenue YTD by business unit?", "sessionId": "s1"}'

# Budget variance
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Which business units are over budget for Q3?", "sessionId": "s1"}'

# Overdue invoices
curl -sN http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"query": "Show me all overdue invoices over $10,000", "sessionId": "s1"}'
```

## Demo Mode (No API Key)

```bash
docker compose up -d
./mvnw -pl datasquire-example-app spring-boot:run -Dspring-boot.run.profiles=demo
./demo/run-demo.sh
```

Pre-recorded responses showcase DataSquire capabilities without an OpenAI key.
See [DEMO.md](DEMO.md) for detailed walkthrough.

## Configuration

All settings in `src/main/resources/application.yml`. Key overrides:

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | (required) | OpenAI API key |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/financedb | DB connection |
| `datasquire.orchestrator.max-iterations` | 5 | Max retry loops per query |
| `datasquire.sql-executor.max-rows` | 50 | Row limit on results |

## Architecture

```
User Question (natural language)
     ↓
[DataSquire Orchestrator]
     ↓
[Schema RAG] → embeds query → cosine search pgvector → top-8 schema chunks
     ↓
[Text-to-SQL Agent] → generates + validates SQL
     ↓
[SQL Executor] → runs read-only query (statement timeout enforced)
     ↓
[Response Synthesizer] → natural language answer with business context
```

## Schema Overview

| Table | Purpose |
|-------|---------|
| business_units | 6 organizational divisions across 3 regions |
| chart_of_accounts | 30 GL accounts (assets, liabilities, equity, revenue, expenses) |
| journal_entries | 200 accounting transactions over FY2024 |
| journal_lines | 400 double-entry line items linking entries to accounts and BUs |
| budgets | Quarterly budget allocations per BU per account |
| invoices | 80 vendor invoices (paid, outstanding, overdue, disputed) |
| cash_flow | 50 cash movements (operating, investing, financing) |
