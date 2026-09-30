# DisputeDesk

Reference implementation for the blog series
**"Architecting and Implementing AI-Native Enterprise Systems in Java"** by [Dhaval Shah](https://www.dhaval-shah.com/about/).

One system, built one layer per part: an AI dispute-resolution agent for a fictional card issuer,
**Meridian Bank**, on Java 25, Spring Boot 4 and Spring AI 2.

> All data, merchants and reason codes are fictional. Nothing here reflects any real card network's rules.

## The series

| Part | Topic | What DisputeDesk gains | Tag |
|------|-------|------------------------|-----|
| 0 | What & why of AI-native systems, and why the JVM | The existing deterministic dispute system | `part-00` |
| 1 | Why deterministic thinking breaks | Complaint classifier + variance experiment | `part-01` |
| 2 | Anatomy of an agent | Hand-written agent loop, first tool, structured output | `part-02` |

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

## Run Stage 1
```shell
    mvn -pl dispute-agent spring-boot:run "-Dspring-boot.run.profiles=experiment,local" "-Dspring-boot.run.arguments=--experiment.runs=1"
```
C4, C5 and C10 bounded still showing unknown. Hence update the rules accordingly

## Run Stage 2
```shell
    mvn -pl dispute-agent spring-boot:run "-Dspring-boot.run.profiles=experiment,local" "-Dspring-boot.run.arguments=--experiment.runs=5"
```
C4, C5 and C10 should be showing correct reason code i.e. D201

## Run Stage 3
```shell
    export GROQ_API_KEY=gs_
    mvn -pl dispute-agent spring-boot:run \
        -Dspring-boot.run.profiles=experiment,groq \
        -Dspring-boot.run.arguments=--experiment.runs=20
```


```ps
    $env:GROQ_API_KEY = "gs_"
    echo $env:GROQ_API_KEY
    mvn -pl dispute-agent spring-boot:run "-Dspring-boot.run.profiles=experiment,groq" "-Dspring-boot.run.arguments=--experiment.runs=2"        
```


Writes `dispute-agent/target/variance.csv` and prints a markdown summary.
800 model calls at the default settings - start with `--experiment.runs=5`.
Add the `local` profile to run against Ollama instead.

## Pinned versions (unchanged for the whole series)

Java 25 · Spring Boot 4.1.x · Spring AI 2.0.x · PostgreSQL 17 + pgvector
