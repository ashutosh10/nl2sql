package com.example.nl2sql.agents;

import com.example.nl2sql.calcite.CalciteSqlCompiler;
import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.core.semantic.SemanticModels;

public class SqlCompilerAgent implements Agent {
  public static final String NAME = "sql-compiler";
  private final CalciteSqlCompiler compiler = new CalciteSqlCompiler();
  private final SemanticModels.Registry registry;
  private final String dialect;

  public SqlCompilerAgent(SemanticModels.Registry registry, String dialect) {
    this.registry = registry;
    this.dialect = dialect == null ? "postgres" : dialect;
  }

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    LogicalMetricQuery q = (LogicalMetricQuery) request.getPayload();
    String sql = compiler.compile(q, registry, dialect);
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, sql);
  }
}
