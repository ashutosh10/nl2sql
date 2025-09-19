package com.example.nl2sql.core.llm;

public class ChatOptions {
  private String model;
  private Double temperature;
  private Integer maxTokens;
  private boolean jsonMode;

  public String getModel() { return model; }
  public ChatOptions setModel(String model) { this.model = model; return this; }
  public Double getTemperature() { return temperature; }
  public ChatOptions setTemperature(Double temperature) { this.temperature = temperature; return this; }
  public Integer getMaxTokens() { return maxTokens; }
  public ChatOptions setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
  public boolean isJsonMode() { return jsonMode; }
  public ChatOptions setJsonMode(boolean jsonMode) { this.jsonMode = jsonMode; return this; }
}
