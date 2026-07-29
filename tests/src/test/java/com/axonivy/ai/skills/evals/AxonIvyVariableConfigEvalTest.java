package com.axonivy.ai.skills.evals;

import static com.axonivy.ai.skills.judge.LlmAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import com.axonivy.ai.skills.agent.CopilotAgentRunner;
import com.axonivy.ai.skills.agent.Workspace;
import com.axonivy.ai.skills.judge.CopilotJudge;

class AxonIvyVariableConfigEvalTest {

  private static final Path SKELETON_PROJECT = Path.of("src/test/fixtures/ivy-project");
  private static final String SKILL = "axon-ivy-variable-config";
  private static final String VARIABLES_YAML = "config/variables.yaml";
  private static final String RUN_MODEL = "gpt-5.4-mini";
  private static final String JUDGE_MODEL = "gpt-5.4-mini";

  @TempDir
  Path tempDir;

  private static CopilotAgentRunner runner;
  private static CopilotJudge judge;

  @BeforeAll
  static void setup() {
    String testedSkillsDir = Path.of("..").toAbsolutePath().normalize().toString();
    runner = new CopilotAgentRunner(RUN_MODEL, testedSkillsDir);
    judge = new CopilotJudge(JUDGE_MODEL);
  }

  @AfterAll
  static void stop() {
    if (runner != null) {
      runner.close();
    }

    if (judge != null) {
      judge.close();
    }
  }

  @ParameterizedTest(name = "firesOnAVariableRequest_enableSkills={0}")
  @ValueSource(booleans = { true, false })
  void firesOnAVariableRequest(boolean enableSkills) {
    var workspace = Workspace.materialize(SKELETON_PROJECT, tempDir);

    var run = runner.run("add a new variable called PageSize and set default value to 10 and also a short comment to describe it",
        workspace.root(), enableSkills);

    // System.out.println("Run result: " + run.transcript());
    // System.out.println("Run usage: " + run.usage());
    // System.out.println(workspace.read(VARIABLES_YAML));

    assertTrue(run.usage().outputTokens() < 15_000);
    assertTrue(run.elapsed().toMinutes() < 5);

    // llm-as-judge check
    assertThat(judge, workspace.read(VARIABLES_YAML))
        .satisfies("the comment explain clearly in software application context what the PageSize variable is.");

    // deterministic check
    assertEquals(enableSkills, run.invoked(SKILL));
    assertDoesNotThrow(() -> {
      new Yaml().load(workspace.read(VARIABLES_YAML));
    });
    assertTrue(workspace.read(VARIABLES_YAML).contains("PageSize: \"10\""));
  }
}
