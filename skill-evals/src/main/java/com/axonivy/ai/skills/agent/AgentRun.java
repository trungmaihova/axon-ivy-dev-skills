package com.axonivy.ai.skills.agent;

import java.time.Duration;
import java.util.Set;

public record AgentRun(
    String transcript,
    Set<InvokedSkill> invokedSkills,
    Usage usage,
    Duration elapsed) {

  public boolean invoked(String skillName) {
    return invokedSkills.stream().anyMatch(s -> s.name().equals(skillName));
  }
}
