package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import java.util.*;

public class DomainClassifierAgent implements Agent {
  public static final String NAME = "domain-classifier";

  private final Map<String, String> keywordToDomain = Map.of(
      "deploy", "deployments",
      "deployment", "deployments",
      "terraform", "deployments",
      "repo", "repositories",
      "branch", "branches"
  );

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    String text = String.valueOf(request.getPayload()).toLowerCase(Locale.ROOT);
    Set<String> evidence = new LinkedHashSet<>();
    Map<String, Integer> scoreMap = new HashMap<>();
    for (var e: keywordToDomain.entrySet()) {
      if (text.contains(e.getKey())) {
        scoreMap.merge(e.getValue(), 1, Integer::sum);
        evidence.add(e.getKey());
      }
    }
    String domain = scoreMap.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("general");
    double score = scoreMap.getOrDefault(domain, 0) / 3.0; // simple heuristic

    Map<String,Object> resp = new LinkedHashMap<>();
    resp.put("domain", domain);
    resp.put("score", score);
    resp.put("evidence", evidence);
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, resp);
  }
}
