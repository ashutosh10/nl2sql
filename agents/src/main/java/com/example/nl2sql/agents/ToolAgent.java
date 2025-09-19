package com.example.nl2sql.agents;

import com.example.nl2sql.core.agent.Agent;
import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.mcp.McpCall;
import com.example.nl2sql.core.tool.Tool;
import com.example.nl2sql.agents.tools.LlmTool;
import com.example.nl2sql.agents.tools.EmbeddingsTool;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// MCP tool stub: deterministic behavior for llm and embeddings
public class ToolAgent implements Agent {
  public static final String NAME = "tool-agent";
  private final Map<String, Tool> tools = new ConcurrentHashMap<>();

  public ToolAgent() {
    // Register default tools
    register(new LlmTool());
    register(new EmbeddingsTool());
  }

  public void register(Tool tool) {
    tools.put(tool.name().toLowerCase(Locale.ROOT), tool);
  }

  @Override public String name() { return NAME; }

  @Override public A2AEnvelope<?> handle(A2AEnvelope<?> request) {
    McpCall call = (McpCall) request.getPayload();
    Object result;
    try {
      String toolName = call.getTool() == null ? "" : call.getTool().toLowerCase(Locale.ROOT);
      Tool tool = tools.get(toolName);
      if (tool == null) {
        result = "unsupported_tool:" + toolName;
      } else {
        result = tool.call(call.getMethod(), call.getArgs());
      }
    } catch (Exception e) {
      result = "error:" + e.getMessage();
    }
    return new A2AEnvelope<>(NAME, request.getFrom(), A2AEnvelope.Type.response, result);
  }
}
