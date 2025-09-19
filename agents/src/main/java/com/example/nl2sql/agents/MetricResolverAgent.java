package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.intent.Filter;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.core.semantic.SemanticModels;
import com.example.nl2sql.semantic.RegistryLoader;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class MetricResolverAgent implements Agent {
  public static final String NAME = "metric-resolver";

  private final SemanticModels.Registry registry;

  public MetricResolverAgent(SemanticModels.Registry registry) {
    this.registry = registry;
  }

  public static MetricResolverAgent fromResource(String resource) {
    RegistryLoader loader = new RegistryLoader();
    return new MetricResolverAgent(loader.loadFromClasspath(resource));
  }

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    Intent intent = (Intent) request.getPayload();
    LogicalMetricQuery lmq = resolve(intent);
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, lmq);
  }

  private LogicalMetricQuery resolve(Intent intent) {
    LogicalMetricQuery q = new LogicalMetricQuery();
    // MVP: support deployment_frequency metric on deployments entity
    SemanticModels.Metric m = registry.findMetric(intent.getMetric());
    if (m == null) throw new IllegalArgumentException("Unknown metric: " + intent.getMetric());
    // choose base entity: prefer first of entities[], fallback to legacy single entity
    String baseEntity = (m.getEntities() != null && !m.getEntities().isEmpty()) ? m.getEntities().get(0) : m.getEntity();
    q.setEntity(baseEntity);
    q.setMetricExpr(m.getExpression());

    // Joins for known dims
    q.getSelect().add("COUNT(*) AS deployment_frequency");
    for (String g : intent.getGroupBy()) {
      q.getSelect().add(g + " AS " + g.replace('.', '_'));
      q.getGroupBy().add(g);
      if (g.startsWith("branches.")) {
        q.getJoins().add("deployments -> branches on deployments.branch_id = branches.id");
      }
      if (g.startsWith("repositories.")) {
        q.getJoins().add("deployments -> repositories on deployments.repo_id = repositories.id");
      }
    }

    // Add explicit relations from metric if present
    if (m.getRelations() != null) {
      for (String rel : m.getRelations()) {
        if (rel != null && !rel.isBlank()) q.getJoins().add(rel);
      }
    }

    DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
    if (intent.getTimeRange() != null) {
      if (intent.getTimeGrain() != null) {
        String tf = m.getTimeField();
        String expr = "date_trunc('" + intent.getTimeGrain() + "', " + tf + ")";
        q.getSelect().add(expr + " AS time_bucket");
        q.getGroupBy().add(expr);
      }
      q.getWhere().add(m.getTimeField() + " >= '" + df.format(intent.getTimeRange().getStart()) + "'");
      q.getWhere().add(m.getTimeField() + " <= '" + df.format(intent.getTimeRange().getEnd()) + "'");
    }

    for (Filter f : intent.getFilters()) {
      q.getWhere().add(f.getField() + " " + f.getOp() + " '" + f.getValue() + "'");
      if (f.getField().startsWith("repositories.")) {
        q.getJoins().add("deployments -> repositories on deployments.repo_id = repositories.id");
      }
      if (f.getField().startsWith("branches.")) {
        q.getJoins().add("deployments -> branches on deployments.branch_id = branches.id");
      }
    }

    q.getOrderBy().add("deployment_frequency DESC");
    return q;
  }
}
