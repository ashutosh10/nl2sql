package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.agent.AgentRegistry;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.core.query.QueryResult;

public class OrchestratorAgent implements Agent {
  private final AgentRegistry registry;
  public static final String NAME = "orchestrator";

  public OrchestratorAgent(AgentRegistry registry) {
    this.registry = registry;
  }

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    // Orchestrate pipeline
    String text = String.valueOf(request.getPayload());
    // Domain classify
    A2AEnvelope<?> dcResp = registry.send(new A2AEnvelope<>(NAME, DomainClassifierAgent.NAME, A2AEnvelope.Type.request, text));
    // Intent parse
    A2AEnvelope<?> ipResp = registry.send(new A2AEnvelope<>(NAME, IntentParserAgent.NAME, A2AEnvelope.Type.request, text));
    Intent intent = (Intent) ipResp.getPayload();
    // Metric resolve
    A2AEnvelope<?> mrResp = registry.send(new A2AEnvelope<>(NAME, MetricResolverAgent.NAME, A2AEnvelope.Type.request, intent));
    LogicalMetricQuery lmq = (LogicalMetricQuery) mrResp.getPayload();
    // SQL compile
    A2AEnvelope<?> scResp = registry.send(new A2AEnvelope<>(NAME, SqlCompilerAgent.NAME, A2AEnvelope.Type.request, lmq));
    String sql = (String) scResp.getPayload();
    // Execute (mocked)
    A2AEnvelope<?> dbResp = registry.send(new A2AEnvelope<>(NAME, DbExecutorAgent.NAME, A2AEnvelope.Type.request, sql));
    QueryResult result = (QueryResult) dbResp.getPayload();
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, result);
  }
}
