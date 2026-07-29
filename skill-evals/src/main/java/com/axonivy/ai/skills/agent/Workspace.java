package com.axonivy.ai.skills.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.file.PathUtils;

public final class Workspace {
  
  private static final Set<String> EXCLUDED_FILES = Set.of(".git", ".idea", "target", "node_modules", ".mvn");

  private final Path root;

  private Workspace(Path root) { 
    this.root = root;
  }

  public static Workspace materialize(Path fixture, Path into) {
    copyTree(fixture, into);
    return new Workspace(into);
  }

  public Path root() {
    return root;
  }

  public boolean has(String relativePath) {
    return root.resolve(relativePath).toFile().exists();
  }

  public List<String> files(String relativePath) {
    try {
      return Files.walk(root.resolve(relativePath))
        .filter(Files::isRegularFile)
        .map(root::relativize)
        .map(Path::toString)
        .filter(p -> EXCLUDED_FILES.stream().noneMatch(p::contains))
        .sorted()
        .toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String read(String relativePath) {
    try {
      return Files.readString(root.resolve(relativePath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void copyTree(Path source, Path target) {
    try {
      PathUtils.copyDirectory(source, target);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
