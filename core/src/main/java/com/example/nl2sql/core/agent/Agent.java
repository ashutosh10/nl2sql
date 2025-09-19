package com.example.nl2sql.core.agent;

import com.example.nl2sql.core.a2a.A2AEnvelope;

public interface Agent {
  String name();
  A2AEnvelope<?> handle(A2AEnvelope<?> request);
}
