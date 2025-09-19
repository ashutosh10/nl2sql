package com.example.nl2sql.core.observability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class InMemoryEventBus implements EventBus {
  private final List<Consumer<A2AEvent>> consumers = new CopyOnWriteArrayList<>();

  @Override
  public void publish(A2AEvent event) {
    for (var c : consumers) {
      try {
        c.accept(event);
      } catch (Exception ignored) {
      }
    }
  }

  @Override
  public Subscription subscribe(Consumer<A2AEvent> consumer) {
    consumers.add(consumer);
    return () -> consumers.remove(consumer);
  }
}
