package com.example.nl2sql.core.llm;

public class ChatMessage {
  private Role role;
  private String content;

  public ChatMessage() {}
  public ChatMessage(Role role, String content) { this.role = role; this.content = content; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
}
