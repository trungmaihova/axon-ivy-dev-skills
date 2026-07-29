package com.axonivy.ai.skills.judge;

public record Verdict(double score, String explanation) {
  public boolean passes(double threshold) {
    return score >= threshold;
  }
}
