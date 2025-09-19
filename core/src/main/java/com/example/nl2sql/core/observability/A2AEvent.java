package com.example.nl2sql.core.observability;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2AEvent {
  public enum Phase { request_sent, response_received, error }

  private String id;
  private String traceId;
  private String from;
  private String to;
  private String type;
  private Phase phase;
  private long timestampMs;
  private String payloadSummary;
  private String error;
  private Long durationMs;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getTraceId() { return traceId; }
  public void setTraceId(String traceId) { this.traceId = traceId; }
  public String getFrom() { return from; }
  public void setFrom(String from) { this.from = from; }
  public String getTo() { return to; }
  public void setTo(String to) { this.to = to; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Phase getPhase() { return phase; }
  public void setPhase(Phase phase) { this.phase = phase; }
  public long getTimestampMs() { return timestampMs; }
  public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
  public String getPayloadSummary() { return payloadSummary; }
  public void setPayloadSummary(String payloadSummary) { this.payloadSummary = payloadSummary; }
  public String getError() { return error; }
  public void setError(String error) { this.error = error; }
  public Long getDurationMs() { return durationMs; }
  public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
