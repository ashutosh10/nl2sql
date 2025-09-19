package com.example.nl2sql.core.llm;

public class ChatResponse {
  private String content;
  private Integer promptTokens;
  private Integer completionTokens;

  public ChatResponse() {}
  public ChatResponse(String content) { this.content = content; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getPromptTokens() { return promptTokens; }
  public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
  public Integer getCompletionTokens() { return completionTokens; }
  public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
}
