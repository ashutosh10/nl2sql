package com.example.nl2sql.core.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpCall {
  private String tool;      // e.g., "llm", "embeddings", "db"
  private String method;    // e.g., "generateIntent", "embed", "execute"
  private Object args;      // method-specific args JSON

  public McpCall() {}
  public McpCall(String tool, String method, Object args) {
    this.tool = tool; this.method = method; this.args = args;
  }
  public String getTool() { return tool; }
  public void setTool(String tool) { this.tool = tool; }
  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }
  public Object getArgs() { return args; }
  public void setArgs(Object args) { this.args = args; }
}
