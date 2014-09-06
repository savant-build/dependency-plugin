/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.dep
import org.savantbuild.dep.DependencyService
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
/**
 * Delegate for the copy method's closure. This passes through everything to the Copier.
 *
 * @author Brian Pontarelli
 */
class CopyDelegate extends BaseDependencyDelegate {
  public static final String ERROR_MESSAGE = "The dependency plugin copy method must be called like this:\n\n" +
      "  dependency.copy(to: \"some dir\") {\n" +
      "    dependencies(group: \"compile\", transitive: true)" +
      "  }"

  private final DependencyService dependencyService

  public final Path to

  public final boolean removeVersion

  CopyDelegate(Map<String, Object> attributes, Project project, DependencyService dependencyService) {
    super(project)
    this.dependencyService = dependencyService

    if (!GroovyTools.attributesValid(attributes, ["to", "removeVersion"], ["to"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE);
    }

    def to = FileTools.toPath(attributes["to"])
    this.to = project.directory.resolve(to)
    this.removeVersion = attributes["removeVersion"] == true
  }

  /**
   * Copies the dependencies to the target directory
   *
   * @return The number of dependencies copied.
   */
  int copy() {
    if (project.artifactGraph == null || project.workflow == null || resolveConfiguration == null || resolveConfiguration.groupConfigurations.isEmpty()) {
      throw new BuildFailureException("Unable to resolve the project dependencies because one of these items was not specified: " +
          "[project.artifactGraph], [project.workflow], [resolveConfiguration], [resolveConfiguration.groupConfigurations]. " +
          "These are often supplied by by a closure like this:\n\n" +
          "  copy(to: \"foo\") {\n" +
          "    dependencies(group: \"compile\", transitive: true, fetchSource: false, transitiveGroups: [\"compile\"])\n" +
          "  }")
    }

    if (!Files.isDirectory(to)) {
      Files.createDirectories(to)
    }

    ResolvedArtifactGraph resolvedGraph = dependencyService.resolve(project.artifactGraph, project.workflow, resolveConfiguration)
    int count = 0
    resolvedGraph.traverse(resolvedGraph.root, true, { origin, destination, value, depth ->
      String name = destination.file.getFileName().toString()
      if (removeVersion) {
        name = name.replace("-${destination.version}", "")
      }

      count++
      Files.copy(destination.file, to.resolve(name), StandardCopyOption.REPLACE_EXISTING)
      return true
    });

    return count
  }
}
