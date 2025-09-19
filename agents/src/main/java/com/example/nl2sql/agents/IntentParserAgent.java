package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.intent.Filter;
import com.example.nl2sql.core.intent.Intent;
import com.example.nl2sql.core.agent.AgentRegistry;
import com.example.nl2sql.core.mcp.McpCall;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Locale;

public class IntentParserAgent implements Agent {
  public static final String NAME = "intent-parser";
  private final AgentRegistry registry;
  private final ObjectMapper mapper = new ObjectMapper();

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    String text = String.valueOf(request.getPayload());
    Intent intent = llmParseFallbackRuleBased(text);
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, intent);
  }

  // Deterministic rule-based MVP with TODO hook to call LLM via MCP
  private Intent ruleBasedParse(String text) {
    String lower = text.toLowerCase(Locale.ROOT);
    Intent intent = new Intent();
    intent.setMetric("deployment_frequency");

    if (lower.contains("repo ")) {
      // naive parse repo name after "repo " until space
      int idx = lower.indexOf("repo ") + 5;
      String repo = text.substring(idx).split("[ ?]")[0];
      intent.getFilters().add(new Filter("repositories.name", "=", repo));
    }
    if (lower.contains("terraform")) {
      intent.getFilters().add(new Filter("tool", "=", "terraform"));
    }
    if (lower.contains("july 2025")) {
      intent.setTimeRange(new Intent.TimeRange(LocalDate.of(2025,7,1), LocalDate.of(2025,7,31)));
      intent.setTimeGrain("week");
    }
    intent.getGroupBy().add("branches.name");
    return intent;
  }

  // First try LLM via ToolAgent (MCP). Fallback to rule-based on any exception.
  private Intent llmParseFallbackRuleBased(String text) {
    if (registry == null) {
      return ruleBasedParse(text);
    }
    try {
      McpCall call = new McpCall("llm", "generateIntent", text);
      A2AEnvelope<?> toolResp = registry.send(new A2AEnvelope<>(NAME, ToolAgent.NAME, A2AEnvelope.Type.request, call));
      Object payload = toolResp.getPayload();
      if (payload == null) return ruleBasedParse(text);
      String json = String.valueOf(payload);
      Intent parsed = mapper.readValue(json, Intent.class);
      // ensure lists are non-null even if LLM omitted them
      if (parsed.getFilters() == null) parsed.setFilters(new java.util.ArrayList<>());
      if (parsed.getGroupBy() == null) parsed.setGroupBy(new java.util.ArrayList<>());
      return parsed;
    } catch (Exception e) {
      return ruleBasedParse(text);
    }
  }

  public IntentParserAgent() { this.registry = null; }
  public IntentParserAgent(AgentRegistry registry) { this.registry = registry; }
}
