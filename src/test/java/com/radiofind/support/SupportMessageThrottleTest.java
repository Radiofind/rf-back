package com.radiofind.support;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupportMessageThrottleTest {

  @Test
  void sixthMessageWithinAnHourIsRefused() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-11-20T10:00:00Z"));
    SupportMessageThrottle throttle = new SupportMessageThrottle(clock);
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");

    boolean acquired = throttle.tryAcquire("user:7");

    assertFalse(acquired);
  }

  @Test
  void messagesAreAllowedAgainOnceTheOldestLeftTheHour() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(
        Instant.parse("2026-11-20T10:00:00Z"),
        Instant.parse("2026-11-20T10:10:00Z"),
        Instant.parse("2026-11-20T10:20:00Z"),
        Instant.parse("2026-11-20T10:30:00Z"),
        Instant.parse("2026-11-20T10:40:00Z"),
        Instant.parse("2026-11-20T11:00:00Z"));
    SupportMessageThrottle throttle = new SupportMessageThrottle(clock);
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");

    boolean acquired = throttle.tryAcquire("user:7");

    assertTrue(acquired);
  }

  @Test
  void limitIsCountedPerSender() {
    Clock clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-11-20T10:00:00Z"));
    SupportMessageThrottle throttle = new SupportMessageThrottle(clock);
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");
    throttle.tryAcquire("user:7");

    boolean acquired = throttle.tryAcquire("user:8");

    assertTrue(acquired);
  }
}
