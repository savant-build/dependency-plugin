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
import org.savantbuild.lang.Classpath
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import java.nio.file.Path

/**
 * Classpath delegate for closures.
 *
 * @author Brian Pontarelli
 */
class ClasspathDelegate extends BaseDependencyDelegate {
  public static final String ERROR_MESSAGE = "The dependency plugin classpath method must be called like this:\n\n" +
      "  dependency.classpath {\n" +
      "    path(location: \"some-directory\")\n" +
      "    path(location: Paths.get(\"some-otherdirectory\"))\n" +
      "  }"

  private final DependencyService dependencyService

  List<Path> paths = new ArrayList<>()

  ClasspathDelegate(Project project, DependencyService dependencyService) {
    super(project)
    this.dependencyService = dependencyService
  }

  /**
   * Adds a path to the classpath. This is called with name attributes (location is required) like this:
   *
   * <pre>
   *   path(location: "some-directory")
   * </pre>
   *
   * @param attributes The named attributes.
   * @return The Path.
   */
  Path path(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["location"], ["location"], [:])) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    def location = attributes["location"]
    def path = FileTools.toPath(location)
    paths.add(project.directory.resolve(path).toAbsolutePath())
    return path
  }

  /**
   * Converts the delegate to a {@link Classpath} instance.
   *
   * @return The Classpath.
   */
  Classpath toClasspath() {
    Classpath classpath
    if (traversalRules != null && traversalRules.rules.size() > 0 && project.dependencies != null) {
      ResolvedArtifactGraph graph = dependencyService.resolve(project.artifactGraph, project.workflow, traversalRules)
      classpath = graph.toClasspath()
    } else {
      classpath = new Classpath()
    }

    if (paths.size() > 0) {
      paths.each { path -> classpath.path(path) }
    }

    return classpath
  }
}
