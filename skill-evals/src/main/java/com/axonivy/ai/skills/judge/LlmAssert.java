package com.axonivy.ai.skills.judge;

public final class LlmAssert {

  public static final double DEFAULT_THRESHOLD = 0.8;

  private final Judge judge;
  private final String responseToGrade;
  private double threshold = DEFAULT_THRESHOLD;

  private LlmAssert(Judge judge, String responseToGrade) {
    this.judge = judge;
    this.responseToGrade = responseToGrade;
  }

  public static LlmAssert assertThat(Judge judge, String responseToGrade) {
    return new LlmAssert(judge, responseToGrade);
  }

  public LlmAssert withThreshold(double threshold) {
    this.threshold = threshold;
    return this;
  }

  public LlmAssert satisfies(String criteria) {
    Verdict verdict = judge.evaluate(responseToGrade, criteria);
    if (!verdict.passes(threshold)) {
      throw new AssertionError("""
          LLM-as-judge: criteria not satisfied.
          Criteria: %s
          Threshold: %s
          Actual score: %s
          Explanation: %s
          """.formatted(criteria, threshold, verdict.score(), verdict.explanation()));
    }
    return this;
  }
}
