package com.example.nl2sql.core.llm;

import java.util.List;
import java.util.stream.Collectors;

public class MockLlmClient implements LlmClient {
  @Override
  public ChatResponse chat(List<ChatMessage> messages, ChatOptions options) {
    // Simple deterministic behavior for tests/demo
    String joined = messages.stream()
        .map(m -> "[" + m.getRole() + "] " + m.getContent())
        .collect(Collectors.joining("\n"));
    if (options != null && options.isJsonMode()) {
      // If json mode, return fixed minimal JSON for intent
      return new ChatResponse("{\"metric\":\"deployment_frequency\"}");
    }
    return new ChatResponse(joined);
  }
}
