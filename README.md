# DataSquire

**Your AI squire for enterprise data.**

[![Build](https://img.shields.io/github/actions/workflow/status/datasquire/datasquire/ci.yml?branch=main)](https://github.com/datasquire/datasquire/actions)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green.svg)](https://spring.io/projects/spring-boot)

<!-- Logo placeholder: place logo at docs/logo.png and uncomment -->
<!-- <p align="center"><img src="docs/logo.png" width="280" alt="DataSquire logo"></p> -->

---

Add one Spring Boot dependency. Write a schema doc. Your users ask questions in English, get answers from SQL.

---

## 30-Second Demo

**application.yml**

```yaml
datasquire:
  enabled: true
  schema-rag:
    namespace: mydb
    top-k: 8
  sql-executor:
    max-rows: 50
    statement-timeout-seconds: 30

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: readonly_user
    password: ${DB_PASSWORD}
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.3
```

**Ask a question:**

```bash
# Create a session
SESSION=$(curl -s http://localhost:8080/api/sessions -X POST | jq -r '.sessionId')

# Query in plain English
curl -s http://localhost:8080/api/sessions/$SESSION/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the top 5 customers by total spend?"}' | jq
```

**Response:**

```json
{
  "answer": "Here are the top 5 customers by total spend:",
  "data": [
    {"name": "Eleanor Hunt", "total_spend": "$211.55"},
    {"name": "Karl Seal", "total_spend": "$208.58"},
    {"name": "Clara Shaw", "total_spend": "$195.67"},
    {"name": "Marion Snyder", "total_spend": "$194.61"},
    {"name": "Rhonda Kennedy", "total_spend": "$191.62"}
  ],
  "sql": "SELECT c.first_name || ' ' || c.last_name AS name, SUM(p.amount) ... ",
  "confidence": 0.92
}
```

---

## Features

- **RAG-powered schema retrieval** -- Embeds your schema docs into pgvector; retrieves only relevant tables per query
- **Text-to-SQL generation** -- LLM generates SQL grounded in retrieved schema context
- **Enterprise safety by default**
  - Read-only connection enforcement (no mutations possible)
  - Statement timeout (kills long-running queries)
  - Row and column output caps (prevents data floods)
  - Scope enforcement (restricts what domains users can query)
  - Response filtering with PII redaction hooks
- **SSE streaming** -- Stream partial answers to the frontend as they generate
- **Session memory** -- Multi-turn conversations with context carry-over
- **Pluggable everything** -- SPI interfaces for schema retrieval, scope rules, response filters, session storage
- **Multi-agent routing** -- (coming soon) Route queries to specialized agents based on intent
- **One-dependency starter** -- Single `datasquire-spring-boot-starter` brings in all modules with auto-configuration

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.datasquire</groupId>
    <artifactId>datasquire-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

> DataSquire is not yet on Maven Central. Clone the repo and `mvn install` locally for now.

### 2. Configure your datasource and LLM

Add to `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/yourdb
    username: readonly_user
    password: ${DB_PASSWORD}
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}

datasquire:
  enabled: true
  schema-rag:
    namespace: yourdb
```

### 3. Write a schema document

Create `src/main/resources/schema/yourdb-reference.md` describing your tables, columns, relationships, and common query patterns. DataSquire chunks and embeds this into the vector store.

```markdown
## customers table
- customer_id (PK)
- first_name, last_name
- email (unique)
- created_at

Relationships: one customer has many orders.
Common queries: customer lookup by email, customers by signup month.
```

### 4. Run schema ingestion

On first startup, DataSquire auto-ingests schema docs into pgvector. You can also trigger re-ingestion via:

```bash
curl -X POST http://localhost:8080/api/admin/ingest
```

### 5. Start asking questions

```bash
SESSION=$(curl -s http://localhost:8080/api/sessions -X POST | jq -r '.sessionId')
curl -s "http://localhost:8080/api/sessions/$SESSION/query" \
  -H "Content-Type: application/json" \
  -d '{"question": "How many orders were placed last month?"}'
```

---

## Architecture

```
                         +------------------+
                         |   HTTP / SSE     |
                         +--------+---------+
                                  |
                         +--------v---------+
                         |   Orchestrator   |
                         +--------+---------+
                                  |
                  +---------------+---------------+
                  |                               |
         +--------v---------+           +--------v---------+
         |    SubAgent      |           |    SubAgent      |
         |  (Text-to-SQL)   |           |   (coming soon)  |
         +--------+---------+           +------------------+
                  |
       +----------+----------+
       |                     |
+------v-------+    +--------v--------+
| SchemaProvider|    |  ScopeEnforcer  |
|  (RAG/pgvec) |    |  (policy check) |
+--------------+    +-----------------+
       |
+------v-------+
| SQL Executor |
| (read-only)  |
+------+-------+
       |
+------v-------+
|  PostgreSQL  |
+--------------+
```

---

## Configuration Reference

All properties are prefixed with `datasquire.`

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Master on/off switch |
| `orchestrator.max-iterations` | `5` | Max LLM retry loops per query |
| `orchestrator.quality-threshold` | `0.7` | Minimum confidence to return a result |
| `orchestrator.timeout-seconds` | `60` | Total timeout for a single query |
| `schema-rag.enabled` | `true` | Enable RAG-based schema retrieval |
| `schema-rag.top-k` | `8` | Number of schema chunks to retrieve |
| `schema-rag.namespace` | -- | Namespace for vector store isolation |
| `sql-executor.max-rows` | `50` | Maximum rows returned from queries |
| `sql-executor.max-columns` | `20` | Maximum columns in result set |
| `sql-executor.max-output-chars` | `16000` | Character cap on serialized output |
| `sql-executor.statement-timeout-seconds` | `30` | PostgreSQL statement_timeout |
| `session.max-size` | `100` | Max messages per session |
| `session.ttl-minutes` | `30` | Session expiry after last activity |
| `controller.base-path` | `/api` | REST controller base path |

---

## Extension Points

Implement any SPI interface and register it as a Spring bean to override default behavior.

| Interface | Package | Purpose |
|-----------|---------|---------|
| `SubAgent` | `io.datasquire.core.agent` | Add custom query-handling agents |
| `SchemaProvider` | `io.datasquire.core.schema` | Custom schema retrieval (JDBC introspection, remote catalog) |
| `SessionStore` | `io.datasquire.core.session` | Persist sessions to Redis, database, etc. |
| `ScopeEnforcer` | `io.datasquire.core.scope` | Define query access boundaries per user/tenant |
| `ResponseFilter` | `io.datasquire.core.filter` | Redact PII, block sensitive results, audit logging |

Example -- custom PII filter:

```java
@Component
public class PiiRedactionFilter implements ResponseFilter {

    @Override
    public FilterResult filter(String response, RequestContext context) {
        String redacted = PiiDetector.redact(response);
        return FilterResult.modified(redacted);
    }

    @Override
    public int getOrder() {
        return 100; // runs after default filters
    }
}
```

---

## Why DataSquire?

| | DataSquire | Vanna.ai | LangChain SQL | Raw Spring AI |
|---|---|---|---|---|
| Language | Java 21 | Python | Python | Java |
| Framework integration | Spring Boot auto-config | Standalone | Standalone | Manual wiring |
| Schema retrieval | RAG over docs (pgvector) | Training on queries | Agent-based | DIY |
| Safety guardrails | Built-in (5 layers) | Minimal | Minimal | None |
| Scope enforcement | SPI-based policy | None | None | None |
| Session memory | Built-in | None | Manual | Manual |
| Streaming | SSE out of the box | None | Callbacks | Manual |
| Enterprise deployment | Spring ecosystem, JVM observability | pip install | pip install | Manual |
| Extensibility | SPI interfaces, Spring beans | Subclass | Chains | N/A |

---

## Modules

| Module | Description |
|--------|-------------|
| `datasquire-core` | SPI interfaces, domain model, shared types |
| `datasquire-schema-rag` | RAG-based schema retrieval using pgvector embeddings |
| `datasquire-text-to-sql` | LLM-powered SQL generation with validation |
| `datasquire-orchestrator` | Query orchestration, retry logic, confidence scoring |
| `datasquire-ingest` | Schema document chunking and vector store ingestion |
| `datasquire-spring-boot-starter` | Auto-configuration, one-dependency entry point |
| `datasquire-example-app` | Working example with the pagila DVD rental database |

---

## Roadmap

- [ ] Multi-agent routing (intent classification, specialized agents)
- [ ] Maven Central publication
- [ ] Web UI for interactive querying and schema management
- [ ] Ollama/local LLM support (air-gapped deployments)
- [ ] Additional database support (MySQL, Oracle)
- [ ] Query caching and result memoization
- [ ] OpenTelemetry instrumentation
- [ ] MCP (Model Context Protocol) server mode

---

## Requirements

- Java 21+
- PostgreSQL 15+ with pgvector extension
- An LLM API key (OpenAI, or any Spring AI-compatible provider)

---

## Building from Source

```bash
git clone https://github.com/datasquire/datasquire.git
cd datasquire
./mvnw clean install -DskipTests
```

To run the example app, see [`datasquire-example-app/README.md`](datasquire-example-app/README.md).

---

## License

[Apache License 2.0](LICENSE)

---

## Contributing

Contributions are welcome. Please open an issue before submitting large PRs.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Open a pull request against `main`

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
