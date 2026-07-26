package com.axonivy.ai.skills.judge;

public interface Judge {
  Verdict evaluate(String response, String criteria);
}
