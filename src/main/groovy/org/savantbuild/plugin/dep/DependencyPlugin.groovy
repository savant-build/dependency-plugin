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
import org.savantbuild.dep.domain.Publication
import org.savantbuild.dep.domain.ReifiedArtifact
import org.savantbuild.dep.domain.ResolvedArtifact
import org.savantbuild.dep.domain.Version
import org.savantbuild.dep.graph.DependencyGraph
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Dependency plugin.
 *
 * @author Brian Pontarelli
 */
class DependencyPlugin extends BaseGroovyPlugin {
  DependencyService dependencyService = new DefaultDependencyService(output)

  DependencyPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)

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
   * Copies the project's dependencies to a directory. This delegates to the {@link CopyDelegate} via the closure.
   * The attributes must also contain a "to" directory.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   dependency.copy(to: "build/distributions/lib") {*     dependencies(group: "compile", transitive: true, fetchSource: true, transitiveGroups: ["compile", "runtime"])
   *}* </pre>
   *
   * @param attributes The named attributes (to is required).
   * @param closure The closure.
   * @return The number of dependencies copied.
   */
  int copy(Map<String, Object> attributes, Closure closure) {
    CopyDelegate delegate = new CopyDelegate(attributes, project, dependencyService)
    closure.delegate = delegate
    closure()

    return delegate.copy()
  }

  /**
   * Builds a Classpath with Paths and dependencies. This delegates to the {@link ClasspathDelegate} via the closure.
   * Look at the methods on that class and its base classes to determine how to use the classpath closure.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   Classpath classpath = dependency.classpath {*     dependencies(group: "compile", transitive: true, fetchSource: true, transitiveGroups: ["compile", "runtime"])
   *}* </pre>
   *
   * @param closure The closure.
   * @return The Classpath.
   */
  Classpath classpath(Closure closure) {
    ClasspathDelegate delegate = new ClasspathDelegate(project, dependencyService)
    closure.delegate = delegate
    closure()

    return delegate.toClasspath()
  }

  /**
   * Uses the {@link DependencyService} to resolve the project's dependencies. This invokes the Closure and delegates
   * to a {@link ResolveDelegate}. This method returns the resulting {@link ResolvedArtifactGraph}.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   ResolvedArtifactGraph graph = dependency.resolve {*     dependencies(group: "compile", transitive: true, fetchSource: true, transitiveGroups: ["compile", "runtime"])
   *}* </pre>
   *
   * @param closure The Closure.
   * @return The ResolvedArtifactGraph.
   */
  ResolvedArtifactGraph resolve(Closure closure) {
    ResolveDelegate delegate = new ResolveDelegate(project, dependencyService)
    closure.delegate = delegate
    closure()

    return delegate.resolve()
  }

  /**
   * Integrates the project (using the project's defined publications and workflow). If there are no publications, this
   * does nothing. Otherwise, it builds the integration version from the project's version and then publishes the
   * publications using the project's workflow.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   dependency.integrate()
   * </pre>
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
      ReifiedArtifact artifact = new ReifiedArtifact(publication.artifact.id, integrationVersion, project.license)
      Publication integrationPublication = new Publication(artifact, publication.metaData, publication.file, publication.sourceFile)
      dependencyService.publish(integrationPublication, project.workflow.publishWorkflow)
    }
  }

  /**
   * Determines which of the project's direct dependencies are not being used anymore. This assumes that the groups are
   * broken down like this:
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   dependency.integrate()
   * </pre>
   */
  Set<ResolvedArtifact> listUnusedDependencies(Map<String, Object> attributes = [:]) {
    if (!GroovyTools.attributesValid(attributes, ["mainBuildDirectory", "mainDependencyGroup", "testBuildDirectory", "testDependencyGroup"], [], [:])) {
      fail("Invalid attributes passed to the listUnusedDependencies ${attributes}. The valid attributes are " +
          "[mainBuildDirectory, mainDependencyGroup, testBuildDirectory, testDependencyGroup].");
    }

    Path mainBuildDirectory = FileTools.toPath(attributes["mainBuildDirectory"])
    if (mainBuildDirectory == null) {
      mainBuildDirectory = Paths.get("build/classes/main")
    }
    Path testBuildDirectory = FileTools.toPath(attributes["testBuildDirectory"])
    if (testBuildDirectory == null) {
      testBuildDirectory = Paths.get("build/classes/test")
    }
    String mainDependencyGroup = attributes["mainDependencyGroup"];
    if (mainDependencyGroup == null) {
      mainDependencyGroup = "compile"
    }
    String testDependencyGroup = attributes["testDependencyGroup"];
    if (testDependencyGroup == null) {
      testDependencyGroup = "test-compile"
    }

    DependencyChecker checker = new DependencyChecker(project, output, this)
    Set<ResolvedArtifact> unused = checker.check(mainBuildDirectory, mainDependencyGroup)
    Set<ResolvedArtifact> unusedTest = checker.check(testBuildDirectory, testDependencyGroup)
    unused.addAll(unusedTest)
    return unused
  }
}
