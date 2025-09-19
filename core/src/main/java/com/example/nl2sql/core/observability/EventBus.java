package com.example.nl2sql.core.observability;

import java.util.function.Consumer;

public interface EventBus {
  void publish(A2AEvent event);
  Subscription subscribe(Consumer<A2AEvent> consumer);

  interface Subscription extends AutoCloseable {
    @Override
    void close();
  }
}
