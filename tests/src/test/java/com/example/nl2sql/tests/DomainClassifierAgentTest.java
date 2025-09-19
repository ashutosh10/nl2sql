package com.example.nl2sql.tests;

import com.example.nl2sql.agents.DomainClassifierAgent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DomainClassifierAgentTest {
  @Test
  void classifiesDeploymentsDomain() {
    var agent = new DomainClassifierAgent();
    var resp = agent.handle(new A2AEnvelope<>("test", DomainClassifierAgent.NAME, A2AEnvelope.Type.request,
        "What is the deployment frequency of terraform deployments for repo X?"));
    Map<?,?> payload = (Map<?,?>) resp.getPayload();
    assertEquals("deployments", payload.get("domain"));
    assertTrue(((Number)payload.get("score")).doubleValue() > 0.3);
  }
}
