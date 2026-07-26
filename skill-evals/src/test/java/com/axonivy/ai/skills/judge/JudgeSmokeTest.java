package com.axonivy.ai.skills.judge;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;

class JudgeSmokeTest {
  private static CopilotJudge judge;

  @BeforeAll
  static void setup() {
    judge = new CopilotJudge("gpt-5-mini");
  }

  @AfterAll
  static void stop() {
    if (judge != null) {
      judge.close();
    }
  }

  @Test
  void acceptsAnAnswerThatMeetsTheCriteria() {
    String response = "The capital of France is Paris.";
    String criteria = "The answer must correctly identify the capital of France.";
    LlmAssert.assertThat(judge, response).satisfies(criteria);
  }

  @Test
  void rejectsAnAnswerThatDoesNotMeetTheCriteria() {
    String response = "The capital of France is Berlin.";
    String criteria = "The answer must correctly identify the capital of France.";
    assertThrows(AssertionError.class, () -> {
      LlmAssert.assertThat(judge, response).satisfies(criteria);
    });
  }
}
