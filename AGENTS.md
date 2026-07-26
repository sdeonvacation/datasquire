# AGENTS.md — AI Agent Instructions for DataSquire

## Project Overview

DataSquire is a Java/Spring AI framework for RAG-augmented text-to-SQL + multi-agent orchestration. 
Monorepo: 7 Maven modules (Java backend) + 1 Vite/React module (UI dashboard).

## Tech Stack

- Java 21, Spring Boot 3.4.1, Spring AI 1.0.0
- PostgreSQL 17 + pgvector (schema RAG)
- React 19 + TypeScript + Vite + Tailwind CSS 4
- Maven multi-module build

## Build Commands

```bash
# Java backend (all modules)
mvn compile -s settings.xml
mvn install -s settings.xml -DskipTests
mvn test -s settings.xml  # Requires Docker for integration tests

# Single module
mvn compile -s settings.xml -pl datasquire-text-to-sql

# React UI
cd datasquire-ui && npm install && npm run dev    # Dev server on :3000
cd datasquire-ui && npx tsc --noEmit              # Type check
cd datasquire-ui && npx vite build                # Production build
```

## Module Map

| Module | Purpose | Key Files |
|--------|---------|-----------|
| datasquire-core | SPI interfaces, DTOs (zero deps) | src/.../core/agent/SubAgent.java, core/schema/SchemaProvider.java |
| datasquire-schema-rag | pgvector RAG retrieval | src/.../rag/retriever/SchemaRetriever.java |
| datasquire-text-to-sql | SQL agent + safety enforcement | src/.../sql/executor/SqlExecutorTool.java, SqlSafetyEnforcer.java |
| datasquire-orchestrator | ReAct loop, routing, registry | src/.../orchestrator/react/ReActOrchestrator.java |
| datasquire-spring-boot-starter | Auto-config, SSE controller, session | src/.../starter/controller/QueryController.java |
| datasquire-ingest | Schema chunking + embedding CLI | src/.../ingest/chunker/SchemaChunker.java |
| datasquire-example-app | Finance Controller demo | docker/init.sql, src/.../resources/schema/ |
| datasquire-ui | React dashboard | src/components/, src/hooks/, src/lib/ |

## Architecture Rules

1. **datasquire-core has ZERO external dependencies** — only java.* imports
2. **No circular module dependencies** — core ← rag ← sql, core ← orchestrator, starter aggregates all
3. **All config under `datasquire.*` prefix** in application.yml
4. **SPI pattern everywhere** — @ConditionalOnMissingBean allows override
5. **Read-only SQL enforcement** — SqlSafetyEnforcer + SET LOCAL statement_timeout + DB role

## Security Invariants (NEVER violate)

- SqlSafetyEnforcer scans FULL query for DML keywords (not just first word)
- String literals stripped before scanning (prevents false positives)
- Statement timeout enforced via `SET LOCAL statement_timeout`
- Sessions scoped by tenant: `tenantId + ":" + sessionId`
- Input size validated: query max 10K chars, sessionId max 100 chars
- No secrets in application.yml (use env vars: ${OPENAI_API_KEY}, ${DB_PASSWORD})

## Conventions

- Java records for immutable data, interfaces for SPI contracts
- Static factory methods on response types (SubAgentResponse.success/failure, FilterResult.passThrough/blocked)
- All auto-config beans use @ConditionalOnMissingBean
- React: functional components only, Tailwind classes (no inline styles), lucide-react icons
- React state: single AppContext with useReducer, React.memo on list items

## Test Strategy

- Unit tests: JUnit 5, no Spring context needed for core/orchestrator
- Integration tests: Testcontainers (pgvector:pg17) for SQL executor
- UI tests: Vitest + @testing-library/react
- Docker requirement: Colima or Docker Desktop for integration tests

## Common Tasks

### Adding a new SubAgent
1. Implement `io.datasquire.core.agent.SubAgent` interface
2. Annotate with `@Component` 
3. SubAgentRegistry auto-discovers it
4. Add keywords for routing in `getKeywords()`

### Adding a new extension point
1. Define interface in datasquire-core (zero deps)
2. Provide default impl in relevant module with @ConditionalOnMissingBean
3. Add property to enable/disable in relevant Properties class
4. Document in README configuration table

### Running the demo
```bash
cd datasquire-example-app
docker compose up -d          # Postgres + pgvector + seeded finance data
export OPENAI_API_KEY=sk-...
cd .. && mvn spring-boot:run -pl datasquire-example-app -s settings.xml
# In another terminal:
cd datasquire-ui && npm run dev   # http://localhost:3000
```
