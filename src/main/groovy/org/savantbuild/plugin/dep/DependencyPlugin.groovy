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

import org.savantbuild.dep.DefaultDependencyService
import org.savantbuild.dep.DependencyService
import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Publication
import org.savantbuild.dep.domain.Version
import org.savantbuild.dep.graph.DependencyGraph
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin

import static org.savantbuild.dep.DependencyService.ResolveConfiguration

/**
 * Dependency plugin.
 *
 * @author Brian Pontarelli
 */
class DependencyPlugin extends BaseGroovyPlugin {
  DependencyService dependencyService = new DefaultDependencyService(output)

  DependencyPlugin(Project project, Output output) {
    super(project, output)

    if (!project.dependencies) {
      return
    }

    if (!project.artifactGraph) {
      try {
        DependencyGraph dependencyGraph = dependencyService.buildGraph(project.toArtifact(), project.dependencies, project.workflow)
        project.artifactGraph = dependencyService.reduce(dependencyGraph)
      } catch (e) {
        output.debug(e);
        fail("Unable to resolve project dependencies. Error message is [%s]", e.toString());
      }
    }
  }

  /**
   * Builds a Classpath using the {@link DependencyService} with the given {@link ResolveConfiguration} (if specified).
   * The closure is optional and is invoked with the {@link Classpath} as the delegate. Any method on the {@link Classpath}
   * object can be called from the Closure.
   *
   * @param resolveConfiguration The dependency resolve configuration.
   * @param closure The closure.
   * @return The Classpath.
   */
  Classpath classpath(ResolveConfiguration resolveConfiguration = null,
                      @DelegatesTo(Classpath.class) Closure closure = null) {
    Classpath classpath
    if (resolveConfiguration) {
      ResolvedArtifactGraph resolvedArtifactGraph = resolve(resolveConfiguration)
      if (resolvedArtifactGraph.size() == 0) {
        return new Classpath()
      }

      classpath = resolvedArtifactGraph.toClasspath()
    } else {
      classpath = new Classpath()
    }

    if (closure) {
      closure.delegate = classpath
      closure()
    }

    return classpath
  }

  /**
   * Uses the {@link DependencyService} to resolve the project's dependencies. This method returns the resulting
   * {@link ResolvedArtifactGraph}
   *
   * @param resolveConfiguration The ResolveConfiguration used to resolve the dependencies.
   * @return The ResolvedArtifactGraph.
   */
  ResolvedArtifactGraph resolve(ResolveConfiguration resolveConfiguration) {
    return dependencyService.resolve(project.artifactGraph, project.workflow, resolveConfiguration)
  }

  /**
   * Integrates the project (using the project's defined publications and workflow). If there are no publications, this
   * does nothing. Otherwise, it builds the integration version from the project's version and then publishes the
   * publications using the project's workflow.
   */
  void integrate() {
    if (project.publications.size() == 0) {
      output.info("Project has no publications defined. Skipping integration")
    } else {
      output.info("Integrating project.")
    }

    for (Publication publication : project.publications.allPublications()) {
      // Change the version of the publication to an integration build
      Version integrationVersion = publication.artifact.version.toIntegrationVersion()
      Artifact artifact = new Artifact(publication.artifact.id, integrationVersion, project.license)
      Publication integrationPublication = new Publication(artifact, publication.metaData, publication.file, publication.sourceFile)
      dependencyService.publish(integrationPublication, project.workflow.publishWorkflow)
    }
  }
}
