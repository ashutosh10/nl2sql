package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.query.QueryResult;
import com.example.nl2sql.jdbc.DbExecutor;

public class DbExecutorAgent implements Agent {
  public static final String NAME = "db-executor";
  private final DbExecutor executor = new DbExecutor();

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    String sql = String.valueOf(request.getPayload());
    QueryResult result = executor.execute(sql);
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, result);
  }
}
