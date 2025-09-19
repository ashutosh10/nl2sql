package com.example.nl2sql.tests;

import com.example.nl2sql.agents.MetricResolverAgent;
import com.example.nl2sql.agents.SqlCompilerAgent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.intent.Filter;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.query.LogicalMetricQuery;
import com.example.nl2sql.semantic.RegistryLoader;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class SqlCompilerAgentTest {
  @Test
  void generatesPostgresSqlWithExpectedJoinsAndClauses() {
    var registry = new RegistryLoader().loadFromClasspath("semantic-registry.yml");
    var resolver = new MetricResolverAgent(registry);
    var compiler = new SqlCompilerAgent(registry, "postgres");

    Intent intent = new Intent();
    intent.setMetric("deployment_frequency");
    intent.getFilters().add(new Filter("repositories.name", "=", "repo-X"));
    intent.getFilters().add(new Filter("tool", "=", "terraform"));
    intent.getGroupBy().add("branches.name");
    intent.setTimeRange(new Intent.TimeRange(LocalDate.of(2025,7,1), LocalDate.of(2025,7,31)));
    intent.setTimeGrain("week");

    LogicalMetricQuery q = (LogicalMetricQuery) resolver.handle(new A2AEnvelope<>("test","", A2AEnvelope.Type.request, intent)).getPayload();
    String sql = (String) compiler.handle(new A2AEnvelope<>("test","", A2AEnvelope.Type.request, q)).getPayload();

    String lsql = sql.toLowerCase();
    assertTrue(lsql.contains("from") && lsql.contains("deployments"));
    assertTrue(lsql.contains("join"));
    assertTrue(lsql.contains("date_trunc('week"));
    assertTrue(lsql.contains("group by"));
    assertTrue(lsql.contains("order by"));
  }
}
