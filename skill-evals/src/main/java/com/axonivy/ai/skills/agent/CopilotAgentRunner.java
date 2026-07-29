package com.axonivy.ai.skills.agent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.copilot.CopilotClient;
import com.github.copilot.SystemMessageMode;
import com.github.copilot.generated.AssistantUsageEvent;
import com.github.copilot.rpc.AgentMode;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;

public class CopilotAgentRunner implements AgentRunner, AutoCloseable {

  private static String instructions(Path workspace) {
    return """
        Every file you create or modify MUST be inside %s. 
        Treat that directory as the entire project. Never read, create, edit or delete a file 
        outside it
        """.formatted(workspace.toAbsolutePath());
  }

  private final CopilotClient client;
  private final String model;
  private final String skillsDir;

  private static final Duration RUN_TIMEOUT = Duration.ofMinutes(10);

  public CopilotAgentRunner(String model, String skillsDir) {
    this.client = new CopilotClient();
    this.model = model;
    this.skillsDir = skillsDir;
  }

  @Override
  public AgentRun run(String prompt, Path workingDirectory, boolean enableSkills) {

    var usageEvents = Collections.synchronizedList(new ArrayList<AssistantUsageEvent.AssistantUsageEventData>());

    var config = new SessionConfig()
        .setModel(model)
        .setWorkingDirectory(workingDirectory.toString())
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.APPEND)
            .setContent(instructions(workingDirectory)))
        .setOnEvent(event -> {
          if (event instanceof AssistantUsageEvent usage) {
            usageEvents.add(usage.getData());
          }
        })
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

    if (enableSkills) {
      config.setSkillDirectories(List.of(skillsDir));
    } else {
      config.setEnableSkills(false);
    }

    try (var session = client.createSession(config).get()) {
      long startedAt = System.nanoTime();
      var messageOptions = new MessageOptions()
          .setPrompt(prompt)
          .setAgentMode(AgentMode.AUTOPILOT);
      var result = session.sendAndWait(messageOptions, RUN_TIMEOUT.toMillis()).get();
      var elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

      var skillsFired = session.getRpc().skills.getInvoked().get().skills().stream()
          .map(invokedSkill -> new InvokedSkill(invokedSkill.name(), invokedSkill.path()))
          .collect(Collectors.toSet());

      return new AgentRun(result.getData().content(), skillsFired, Usage.of(usageEvents), elapsed);

    } catch (Exception e) {
      throw new RuntimeException("Failed to run agent", e);
    }
  }

  @Override
  public void close() {
    client.close();
  }

}
