package com.example.nl2sql.tests;

import com.example.nl2sql.agents.MetricResolverAgent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.intent.Filter;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.semantic.RegistryLoader;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class MetricResolverAgentTest {
  @Test
  void resolvesLogicalQueryFromIntent() {
    var registry = new RegistryLoader().loadFromClasspath("semantic-registry.yml");
    var agent = new MetricResolverAgent(registry);

    Intent intent = new Intent();
    intent.setMetric("deployment_frequency");
    intent.getFilters().add(new Filter("repositories.name", "=", "repo-X"));
    intent.getFilters().add(new Filter("tool", "=", "terraform"));
    intent.getGroupBy().add("branches.name");
    intent.setTimeRange(new Intent.TimeRange(LocalDate.of(2025,7,1), LocalDate.of(2025,7,31)));
    intent.setTimeGrain("week");

    LogicalMetricQuery q = (LogicalMetricQuery) agent
        .handle(new A2AEnvelope<>("test", MetricResolverAgent.NAME, A2AEnvelope.Type.request, intent))
        .getPayload();

    assertEquals("deployments", q.getEntity());
    assertTrue(q.getSelect().stream().anyMatch(s -> s.toLowerCase().contains("count(*)")));
    assertTrue(q.getJoins().stream().anyMatch(j -> j.toLowerCase().contains("repositories")));
    assertTrue(q.getJoins().stream().anyMatch(j -> j.toLowerCase().contains("branches")));
    assertTrue(q.getWhere().stream().anyMatch(w -> w.contains("repositories.name = 'repo-X'")));
    assertTrue(q.getWhere().stream().anyMatch(w -> w.contains("tool = 'terraform'")));
    assertTrue(q.getGroupBy().stream().anyMatch(g -> g.toLowerCase().contains("date_trunc('week")));
  }
}
