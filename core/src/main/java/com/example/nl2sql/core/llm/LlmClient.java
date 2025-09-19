package com.example.nl2sql.core.llm;

import java.util.List;

public interface LlmClient {
  ChatResponse chat(List<ChatMessage> messages, ChatOptions options) throws Exception;
}
