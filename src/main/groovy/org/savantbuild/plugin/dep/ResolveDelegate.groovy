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
import org.savantbuild.runtime.BuildFailureException

/**
 * Resolve delegate for closures.
 *
 * @author Brian Pontarelli
 */
class ResolveDelegate extends BaseDependencyDelegate {
  private final DependencyService dependencyService

  ResolveDelegate(Project project, DependencyService dependencyService) {
    super(project)
    this.dependencyService = dependencyService
  }

  /**
   * Converts the delegate to a {@link ResolvedArtifactGraph} instance.
   *
   * @return The ResolvedArtifactGraph.
   */
  ResolvedArtifactGraph resolve() {
    if (project.artifactGraph == null || project.workflow == null || resolveConfiguration == null || resolveConfiguration.groupConfigurations.isEmpty()) {
      throw new BuildFailureException("Unable to resolve the project dependencies because one of these items was not specified: " +
          "[project.artifactGraph], [project.workflow], [resolveConfiguration], [resolveConfiguration.groupConfigurations]. " +
          "These are often supplied by by a closure like this:\n\n" +
          "  resolve() {\n" +
          "    dependencies(group: \"compile\", transitive: true, fetchSource: false, transitiveGroups: [\"compile\"])\n" +
          "  }")
    }

    return dependencyService.resolve(project.artifactGraph, project.workflow, resolveConfiguration)
  }
}
