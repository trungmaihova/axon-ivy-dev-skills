package com.axonivy.ai.skills.agent;

import java.util.List;

import com.github.copilot.generated.AssistantUsageEvent.AssistantUsageEventData;

public record Usage(
  long inputTokens,
  long outputTokens,
  long cacheReadTokens,
  long cacheWriteTokens,
  long reasoningTokens,
  double cost,
  int modelCalls) {

  static final Usage NONE = new Usage(0, 0, 0, 0, 0, 0d, 0);

  static Usage of(List<AssistantUsageEventData> events) {
    long in = 0, out = 0, cacheRead = 0, cacheWrite = 0, reasoning = 0;
    double cost = 0d;
    synchronized (events) {
      for (var e : events) {
        in += orZero(e.inputTokens());
        out += orZero(e.outputTokens());
        cacheRead += orZero(e.cacheReadTokens());
        cacheWrite += orZero(e.cacheWriteTokens());
        reasoning += orZero(e.reasoningTokens());
        cost += e.cost() == null ? 0d : e.cost();
      }
      return new Usage(in, out, cacheRead, cacheWrite, reasoning, cost, events.size());
    }
  }

  private static long orZero(Long value) {
    return value == null ? 0L : value;
  }

  @Override
  public String toString() {
    return "in=" + inputTokens + " out=" + outputTokens + " reasoning=" + reasoningTokens
        + " cacheRead=" + cacheReadTokens + " cacheWrite=" + cacheWriteTokens
        + " cost=" + cost + " calls=" + modelCalls;
  }
}
