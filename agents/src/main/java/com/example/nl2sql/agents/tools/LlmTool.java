package com.example.nl2sql.agents.tools;

import com.example.nl2sql.core.tool.Tool;
import com.example.nl2sql.core.llm.*;
import com.example.nl2sql.core.semantic.SemanticModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LlmTool implements Tool {
  private final LlmClient client;
  private final SemanticModels.Registry registry; // optional context for prompting

  public LlmTool(LlmClient client) { this.client = client; this.registry = null; }
  public LlmTool(LlmClient client, SemanticModels.Registry registry) { this.client = client; this.registry = registry; }
  public LlmTool() { this.client = new MockLlmClient(); this.registry = null; }

  @Override public String name() { return "llm"; }

  @Override public Object call(String method, Object args) throws Exception {
    String m = method == null ? "" : method.toLowerCase();
    switch (m) {
      case "chat":
        return chat(args);
      case "generateintent":
        return generateIntent(args);
      default:
        return "unsupported_method:" + method;
    }
  }

  private Object chat(Object args) throws Exception {
    // Expect args as Map { messages: List<{role, content}>, options?: {model, temperature, maxTokens, jsonMode} }
    if (!(args instanceof Map)) {
      throw new IllegalArgumentException("chat expects Map args with 'messages' and optional 'options'");
    }
    @SuppressWarnings("unchecked")
    Map<String,Object> map = (Map<String,Object>) args;
    Object msgsObj = map.get("messages");
    if (!(msgsObj instanceof List)) throw new IllegalArgumentException("'messages' must be a List");
    @SuppressWarnings("unchecked")
    List<Object> raw = (List<Object>) msgsObj;
    List<ChatMessage> messages = new ArrayList<>();
    for (Object o : raw) {
      if (!(o instanceof Map)) continue;
      @SuppressWarnings("unchecked") Map<String,Object> mm = (Map<String,Object>) o;
      String roleStr = String.valueOf(mm.getOrDefault("role", "user"));
      String content = String.valueOf(mm.getOrDefault("content", ""));
      Role role;
      try { role = Role.valueOf(roleStr); } catch (Exception e) { role = Role.user; }
      messages.add(new ChatMessage(role, content));
    }
    ChatOptions options = buildOptions(map.get("options"));
    ChatResponse resp = client.chat(messages, options);
    return resp.getContent();
  }

  private Object generateIntent(Object args) throws Exception {
    String text = String.valueOf(args);
    StringBuilder sys = new StringBuilder();
    sys.append("You convert user questions into JSON intent for a semantic analytics system. Return ONLY compact JSON.\n");
    if (registry != null) {
      sys.append("Semantic Registry Context:\n");
      for (SemanticModels.Entity e : registry.getEntities()) {
        sys.append("- Entity ").append(e.getName());
        if (e.getDescription() != null && !e.getDescription().isBlank()) {
          sys.append(": ").append(e.getDescription());
        }
        sys.append("\n  Fields:\n");
        for (SemanticModels.Field f : e.getFields()) {
          sys.append("    • ").append(f.getName()).append(" (").append(f.getType()).append(")");
          // Field description if present
          try {
            var desc = f.getDescription();
            if (desc != null && !desc.isBlank()) sys.append(": ").append(desc);
          } catch (Exception ignore) {}
          sys.append("\n");
        }
      }
    }
    List<ChatMessage> messages = List.of(
        new ChatMessage(Role.system, sys.toString()),
        new ChatMessage(Role.user, text)
    );
    ChatOptions options = new ChatOptions().setJsonMode(true);
    ChatResponse resp = client.chat(messages, options);
    return resp.getContent();
  }

  private ChatOptions buildOptions(Object optsObj) {
    ChatOptions opts = new ChatOptions();
    if (!(optsObj instanceof Map)) return opts;
    @SuppressWarnings("unchecked") Map<String,Object> m = (Map<String,Object>) optsObj;
    if (m.get("model") != null) opts.setModel(String.valueOf(m.get("model")));
    if (m.get("temperature") != null) opts.setTemperature(Double.valueOf(String.valueOf(m.get("temperature"))));
    if (m.get("maxTokens") != null) opts.setMaxTokens(Integer.valueOf(String.valueOf(m.get("maxTokens"))));
    if (m.get("jsonMode") != null) opts.setJsonMode(Boolean.parseBoolean(String.valueOf(m.get("jsonMode"))));
    return opts;
  }
}
