package com.example.nl2sql.examples;

import com.example.nl2sql.agents.*;
import com.example.nl2sql.core.agent.AgentRegistry;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.query.QueryResult;
import com.example.nl2sql.semantic.RegistryLoader;

public class Cli {
  public static void main(String[] args) {
    String nl = args.length > 0 ? String.join(" ", args) : "What is the deployment frequency of terraform deployments for repo X in July 2025?";

    // Load semantic registry
    RegistryLoader loader = new RegistryLoader();
    var registry = loader.loadFromClasspath("semantic-registry.yml");

    // Wire agents in-process
    AgentRegistry ar = new AgentRegistry();
    var orchestrator = new OrchestratorAgent(ar);
    var dc = new DomainClassifierAgent();
    var ip = new IntentParserAgent(ar);
    var mr = new MetricResolverAgent(registry);
    var sc = new SqlCompilerAgent(registry, "postgres");
    var db = new DbExecutorAgent();
    var tool = new ToolAgent();

    ar.register(orchestrator);
    ar.register(dc);
    ar.register(ip);
    ar.register(mr);
    ar.register(sc);
    ar.register(db);
    ar.register(tool);

    var resp = ar.send(new A2AEnvelope<>("cli", OrchestratorAgent.NAME, A2AEnvelope.Type.request, nl));
    QueryResult result = (QueryResult) resp.getPayload();

    System.out.println("NL: " + nl);
    // For demo, also compile again to show SQL
    var intentResp = ar.send(new A2AEnvelope<>("cli", IntentParserAgent.NAME, A2AEnvelope.Type.request, nl));
    var lmqResp = ar.send(new A2AEnvelope<>("cli", MetricResolverAgent.NAME, A2AEnvelope.Type.request, intentResp.getPayload()));
    var sqlResp = ar.send(new A2AEnvelope<>("cli", SqlCompilerAgent.NAME, A2AEnvelope.Type.request, lmqResp.getPayload()));
    System.out.println("SQL: " + sqlResp.getPayload());

    System.out.println("Result columns: " + result.getColumns());
    System.out.println("Result rows: " + result.getRows());
  }
}
