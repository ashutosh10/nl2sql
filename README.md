# NL2SQL Multi-Agent Skeleton (Java, Maven)

This project is a modular Java skeleton that converts natural language SDLC questions into optimized SQL via a semantic layer using a multi-agent architecture. Agents communicate via A2A envelopes in-process for MVP and call external tools via an MCP interface (deterministic stubs).

## Modules
- core: A2A/MCP schemas, Agent interface, Intent/LogicalMetricQuery, semantic models, in-memory AgentRegistry.
- semantic-registry: YAML loader (SnakeYAML) to `SemanticModels.Registry`.
- calcite-compiler: Placeholder Calcite compiler (currently a deterministic SQL renderer with TODO to integrate RelBuilder and Postgres dialect).
- jdbc-adapter: JDBC executor (mocked deterministic result, ready for H2/Postgres wiring).
- agents: Orchestrator + DomainClassifier + IntentParser + MetricResolver + SqlCompiler + DbExecutor + Tool Agent stubs.
- examples: CLI runner and sample `semantic-registry.yml`.
- tests: JUnit tests for classifier, resolver, and SQL generation.

## Build
```
mvn -q -DskipITs test
```

## Run the CLI
```
cd examples
mvn exec:java -Dexec.args="What is the deployment frequency of terraform deployments for repo X in July 2025?"
```
Expected output includes generated SQL and a mocked result row `{deployment_frequency=42}`.

## Agent Roles
- OrchestratorAgent: entrypoint; pipelines DomainClassifier → IntentParser → MetricResolver → SqlCompiler → DbExecutor.
- DomainClassifierAgent: keyword-based classifier with placeholders for embedding fallback via ToolAgent.
- IntentParserAgent: rule-based intent extraction with TODO to call LLM via MCP.
- MetricResolverAgent: maps intent to `LogicalMetricQuery` using semantic registry.
- SqlCompilerAgent: compiles logical query to SQL (Calcite integration TODO; simple SQL builder for MVP).
- DbExecutorAgent: executes SQL (mocked; TODO: DataSource properties + JDBC execution).
- ToolAgent: MCP stub for `llm.generateIntent` and `embeddings.embed`.

## A2A & MCP
- A2A envelope: `A2AEnvelope<T>` with id/from/to/type/timestamp/payload.
- MCP: `McpCall(tool, method, args)`; ToolAgent stub returns deterministic values.

## Semantic Registry
Sample YAML at `examples/src/main/resources/semantic-registry.yml` defines entities (deployments, repositories, branches) and metric `deployment_frequency`.

## Extending / TODO
- Replace SQL builder with Apache Calcite RelBuilder and `RelToSqlConverter` (Postgres dialect), add predicate pushdown.
- Implement HTTP endpoints for A2A per agent (Spring Boot/JAX-RS) and replace in-memory router with HTTP/gRPC.
- Implement JDBC executor with HikariCP and Postgres/H2 profiles; add integration tests.
- Security: input validation, SQL injection hardening, RBAC.
- LLM safety and caching; metrics versioning.

## Demo Script
See `examples/demo.sh` for a quick run example.
