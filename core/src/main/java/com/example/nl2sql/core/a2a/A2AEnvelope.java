package com.example.nl2sql.core.a2a;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2AEnvelope<T> {
  public enum Type { request, response, event }

  private String id;
  private String from;
  private String to;
  private Type type;
  private Instant timestamp;
  private T payload;

  public A2AEnvelope() {
    this.id = UUID.randomUUID().toString();
    this.timestamp = Instant.now();
  }

  public A2AEnvelope(String from, String to, Type type, T payload) {
    this();
    this.from = from;
    this.to = to;
    this.type = type;
    this.payload = payload;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getFrom() { return from; }
  public void setFrom(String from) { this.from = from; }
  public String getTo() { return to; }
  public void setTo(String to) { this.to = to; }
  public Type getType() { return type; }
  public void setType(Type type) { this.type = type; }
  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
  public T getPayload() { return payload; }
  public void setPayload(T payload) { this.payload = payload; }
}
