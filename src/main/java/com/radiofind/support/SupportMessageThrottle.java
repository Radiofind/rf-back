package com.radiofind.support;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Component
public class SupportMessageThrottle {

  private static final int MAX_MESSAGES = 5;

  private static final Duration WINDOW = Duration.ofHours(1);

  private static final int SWEEP_THRESHOLD = 10_000;

  private final Clock clock;

  private final Map<String, List<Instant>> sentBySender = new ConcurrentHashMap<>();

  public SupportMessageThrottle(Clock clock) {
    this.clock = clock;
  }

  public boolean tryAcquire(String senderKey) {
    Instant now = clock.instant();
    Instant windowStart = now.minus(WINDOW);
    AtomicBoolean acquired = new AtomicBoolean();

    if (sentBySender.size() > SWEEP_THRESHOLD) {
      sentBySender.values().removeIf(sent -> !sent.getLast().isAfter(windowStart));
    }

    sentBySender.compute(senderKey, (key, sent) -> {
      List<Instant> recent = sent == null
          ? List.of()
          : sent.stream().filter(time -> time.isAfter(windowStart)).toList();

      if (recent.size() >= MAX_MESSAGES) {
        return recent;
      }

      acquired.set(true);
      return Stream.concat(recent.stream(), Stream.of(now)).toList();
    });

    return acquired.get();
  }
}
