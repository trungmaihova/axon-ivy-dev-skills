package com.axonivy.ai.skills.agent;

import java.nio.file.Path;

public interface AgentRunner {

  default AgentRun run(String prompt, Path workingDirectory) {
    return run(prompt, workingDirectory, true);
  }

  AgentRun run(String prompt, Path workingDirectory, boolean enableSkills);
}
