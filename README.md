# DisputeDesk

Reference implementation for the blog series
**"Architecting and Implementing AI-Native Enterprise Systems in Java"** by [Dhaval Shah](https://dhaval-shah.com).

One system, built one layer per part: an AI dispute-resolution agent for a fictional card issuer,
**Meridian Bank**, on Java 25, Spring Boot 4 and Spring AI 2.

> All data, merchants and reason codes are fictional. Nothing here reflects any real card network's rules.

## The series

| Part | Topic | What DisputeDesk gains | Tag |
|------|-------|------------------------|-----|
| 0 | What & why of AI-native systems, and why the JVM | The existing deterministic dispute system | `part-00` |
| 1 | Why deterministic thinking breaks | Complaint classifier + variance experiment | `part-01` |
| 2 | Anatomy of an agent | Hand-written agent loop, first tool, structured output | `part-02` |
| 3 | The Java stack | Spring AI agent, advisors, virtual-thread benchmark | `part-03` |
| 4 | Tools are the new endpoints | Tool design experiment, idempotent credits, first eval | `part-04` |
| 5 | Grounding with RAG | Policy corpus in PGVector, hybrid search, citations | `part-05` |
| 6 | Memory and state | JDBC chat memory, survive-a-restart, cardholder profile | `part-06` |
| 7 | Multi-agent + MCP | Fraud/evidence sub-agents, MCP server and client | `part-07` |
| 8 | Reliability engineering | Eval harness in CI, Resilience4j, chaos test | `part-08` |
| 9 | Security, observability, capstone | Injection suite, OTel → Jaeger, approval gate | `part-09` |

Start with [docs/architecture.md](docs/architecture.md), then [docs/scenarios.md](docs/scenarios.md).

## Run Part 0

Prerequisites: JDK 25, Maven 3.9+, Docker.

```bash
docker compose up -d                           # PostgreSQL 17 + pgvector
mvn -pl dispute-service spring-boot:run        # Flyway creates schema + seed data; API on :8081
```

Then open `http/part-00.http` in IntelliJ (or VS Code REST Client) and run the requests top to bottom.

Tests (Testcontainers starts its own PostgreSQL; Docker must be running):

```bash
mvn verify
```

## Pinned versions (unchanged for the whole series)

Java 25 · Spring Boot 4.1.x · Spring AI 2.0.x · PostgreSQL 17 + pgvector
