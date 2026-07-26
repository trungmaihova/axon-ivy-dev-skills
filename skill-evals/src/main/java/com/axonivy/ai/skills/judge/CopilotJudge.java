package com.axonivy.ai.skills.judge;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import com.github.copilot.CopilotClient;
import com.github.copilot.SystemMessageMode;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;

import tools.jackson.databind.ObjectMapper;

public class CopilotJudge implements Judge, AutoCloseable {

  private static final String SYSTEM_PROMPT = """
      You are a strict evaluator of AI-generated output.
      Judge ONLY from the material you are given — never assume anything you cannot see.
      Do not give the benefit of the doubt: a plausible-sounding claim with no
      substance scores low.
      Reply with JSON only. No prose, no markdown fences.
      """;

  private final String model;
  private final CopilotClient client;
  private final ObjectMapper objectMapper;

  private static final Duration RUN_TIMEOUT = Duration.ofMinutes(10);

  public CopilotJudge(String model) {
    this.model = model;
    this.client = new CopilotClient();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Verdict evaluate(String response, String criteria) {

    String judgePrompt = """
    ## CRITERIA
    %s

    ## OUTPUT TO GRADE
    %s

    ## RESPONSE FORMAT
    Reply with exactly this JSON and nothing else:
    {"score": <number between 0.0 and 1.0>, "explanation": "<one sentence citing specifics>"}
    """.formatted(criteria, response);

    var config = new SessionConfig()
      .setModel(model)
      .setSystemMessage(new SystemMessageConfig().setContent(SYSTEM_PROMPT).setMode(SystemMessageMode.REPLACE))
      .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

    try (var session = client.createSession(config).get()) {
      var msgOptions = new MessageOptions().setPrompt(judgePrompt);
      var result = session.sendAndWait(msgOptions, RUN_TIMEOUT.toMillis()).get();
      return objectMapper.readValue(result.getData().content(), Verdict.class);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Judge call failed", e.getCause());
    }
  }

  @Override
  public void close() {
    client.close();
  }

}
