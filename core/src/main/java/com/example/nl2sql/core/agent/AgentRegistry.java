package com.example.nl2sql.core.agent;

import com.example.nl2sql.core.a2a.A2AEnvelope;
import com.example.nl2sql.core.observability.A2AEvent;
import com.example.nl2sql.core.observability.EventBus;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class AgentRegistry {
  private final Map<String, Agent> agents = new ConcurrentHashMap<>();
  private final EventBus eventBus;

  public AgentRegistry() { this.eventBus = null; }
  public AgentRegistry(EventBus eventBus) { this.eventBus = eventBus; }

  public void register(Agent agent) {
    agents.put(agent.name(), agent);
  }

  public A2AEnvelope<?> send(A2AEnvelope<?> request) {
    if (request.getTo() == null) throw new IllegalArgumentException("Missing 'to' in envelope");
    Agent target = agents.get(request.getTo());
    if (target == null) throw new IllegalArgumentException("Unknown agent: " + request.getTo());
    long start = System.nanoTime();
    String traceId = request.getId() != null ? request.getId() : UUID.randomUUID().toString();
    publishEvent(request, A2AEvent.Phase.request_sent, null, null);
    try {
      A2AEnvelope<?> resp = target.handle(request);
      long durMs = (System.nanoTime() - start) / 1_000_000L;
      publishEvent(resp != null ? resp : request, A2AEvent.Phase.response_received, null, durMs);
      return resp;
    } catch (RuntimeException re) {
      long durMs = (System.nanoTime() - start) / 1_000_000L;
      publishEvent(request, A2AEvent.Phase.error, re.getMessage(), durMs);
      throw re;
    } catch (Exception e) {
      long durMs = (System.nanoTime() - start) / 1_000_000L;
      publishEvent(request, A2AEvent.Phase.error, e.getMessage(), durMs);
      throw e;
    }
  }

  private void publishEvent(A2AEnvelope<?> env, A2AEvent.Phase phase, String error, Long durationMs) {
    if (eventBus == null || env == null) return;
    A2AEvent ev = new A2AEvent();
    ev.setId(env.getId());
    ev.setTraceId(env.getId());
    ev.setFrom(env.getFrom());
    ev.setTo(env.getTo());
    ev.setType(env.getType() != null ? env.getType().name() : null);
    ev.setPhase(phase);
    ev.setTimestampMs(System.currentTimeMillis());
    ev.setDurationMs(durationMs);
    ev.setError(error);
    ev.setPayloadSummary(summarize(env.getPayload()));
    eventBus.publish(ev);
  }

  private String summarize(Object payload) {
    if (payload == null) return "null";
    String s = String.valueOf(payload);
    if (s.length() > 200) s = s.substring(0, 200) + "...";
    return s;
  }
}
