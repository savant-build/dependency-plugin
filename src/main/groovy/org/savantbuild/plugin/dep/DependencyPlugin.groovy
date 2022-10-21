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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.savantbuild.dep.DefaultDependencyService
import org.savantbuild.dep.DependencyService
import org.savantbuild.dep.DependencyTreePrinter
import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Publication
import org.savantbuild.dep.domain.ReifiedArtifact
import org.savantbuild.dep.domain.ResolvedArtifact
import org.savantbuild.dep.graph.DependencyGraph
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

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
      DependencyGraph dependencyGraph = dependencyService.buildGraph(project.toArtifact(), project.dependencies, project.workflow)
      project.artifactGraph = dependencyService.reduce(dependencyGraph)
    }
  }

  /**
   * Copies the project's dependencies to a directory. This delegates to the {@link CopyDelegate} via the closure.
   * The attributes must also contain a "to" directory. If the "to" directory doesn't exist, it is created by this
   * method.
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
  int copy(Map<String, Object> attributes, @DelegatesTo(CopyDelegate.class) Closure closure) {
    CopyDelegate delegate = new CopyDelegate(attributes, project, dependencyService)
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure()

    int count = delegate.copy()
    output.infoln("Copied [${count}] dependencies to [${delegate.to}]")
    return count
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
  Classpath classpath(@DelegatesTo(ClasspathDelegate.class) Closure closure) {
    ClasspathDelegate delegate = new ClasspathDelegate(project, dependencyService)
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure()

    return delegate.toClasspath()
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
      output.infoln("Project has no publications defined. Skipping integration")
    } else {
      output.infoln("Integrating project.")
    }

    for (Publication publication : project.publications.allPublications()) {
      // Change the version of the publication to an integration build
      Version integrationVersion = publication.artifact.version.toIntegrationVersion()
      ReifiedArtifact artifact = new ReifiedArtifact(publication.artifact.id, integrationVersion, project.licenses)
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
   *   dependency.listUnusedDependencies()
   * </pre>
   */
  Set<ResolvedArtifact> listUnusedDependencies(Map<String, Object> attributes = [:]) {
    if (!GroovyTools.attributesValid(attributes, ["mainBuildDirectory", "mainDependencyGroups", "testBuildDirectory", "testDependencyGroups"], [], ["mainDependencyGroups": List.class, "testDependencyGroups": List.class])) {
      fail("Invalid attributes passed to the listUnusedDependencies ${attributes}. The valid attributes are " +
          "[mainBuildDirectory, mainDependencyGroup, testBuildDirectory, testDependencyGroup].")
    }

    Path mainBuildDirectory = FileTools.toPath(attributes["mainBuildDirectory"])
    if (mainBuildDirectory == null) {
      mainBuildDirectory = Paths.get("build/classes/main")
    }
    Path testBuildDirectory = FileTools.toPath(attributes["testBuildDirectory"])
    if (testBuildDirectory == null) {
      testBuildDirectory = Paths.get("build/classes/test")
    }
    List<String> mainDependencyGroups = attributes["mainDependencyGroups"]
    if (mainDependencyGroups == null) {
      mainDependencyGroups = ["compile", "provided"]
    }
    List<String> testDependencyGroups = attributes["testDependencyGroup"]
    if (testDependencyGroups == null) {
      testDependencyGroups = ["test-compile"]
    }

    DependencyChecker checker = new DependencyChecker(project, output, this)
    Set<ResolvedArtifact> unused = checker.check(mainBuildDirectory, mainDependencyGroups)
    Set<ResolvedArtifact> unusedTest = checker.check(testBuildDirectory, testDependencyGroups)
    unused.addAll(unusedTest)
    return unused
  }

  /**
   * Locates the location on disk of a single artifact.
   * <p>
   * Here is an example of calling this method:
   * <p>
   * <pre>
   *   Path path = dependency.path(id: "org.apache.commons:commons-collection:3.1", group: "compile")
   * </pre>
   *
   * @param attributes The named attributes (id and group are required)
   * @return The absolute Path to the dependency on disk or null if the artifact doesn't exist.
   */
  Path path(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["id", "group"], ["id", "group"], ["id": String.class, "group": String.class])) {
      fail("You must supply the name of the dependency group and the dependency id like this:\n\n" +
          "  dependency.absolutePath(id: \"foo:bar:1.0\", group: \"compile\")")
    }

    ResolvedArtifactGraph graph = resolve {
      dependencies(group: attributes["group"].toString(), transitive: false)
    }

    String id = attributes["id"].toString()
    Path path = graph.getPath(new Artifact(id, false).id)
    if (!path) {
      return null
    }

    return path.toAbsolutePath()
  }

  /**
   * Prints out the project dependencies to the output.
   */
  void printFull() {
    DependencyGraph dependencyGraph = dependencyService.buildGraph(project.toArtifact(), project.dependencies, project.workflow)
    DependencyTreePrinter.print(output, dependencyGraph, null, null)
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
  ResolvedArtifactGraph resolve(@DelegatesTo(ResolveDelegate.class) Closure closure) {
    ResolveDelegate delegate = new ResolveDelegate(project, dependencyService)
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure()

    return delegate.resolve()
  }

  /**
   * Writes out all of the project's licenses to the directory given in the [to] attribute. Here is an example of
   * calling this method:
   * <p>
   * <pre>
   *   dependency.writeLicenses(to: "build/licenses")
   * </pre>
   *
   * @param attributes The named attributes (to is required).
   */
  void writeLicenses(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["to", "groups"], ["to"], [:])) {
      fail("You must specify the [to] path where the licenses will be written to. It should look like this:\n\n" +
          "  dependency.writeLicenses(to: \"build/licenses\"")
    }

    Path toDir = FileTools.toPath(attributes["to"])

    // Capture groups the user wants to include
    String[] groups = attributes["groups"]
    Set<String> groupSet = new HashSet<>()
    if (groups != null) {
      groupSet.addAll(Arrays.asList(groups))
    }

    project.artifactGraph.traverse(project.artifactGraph.root, false, null, { origin, destination, group, depth, isLast ->
      // Skip this node and stop traversing down that part of the graph if the group isn't wanted
      if (groupSet.size() > 0 && !groupSet.contains(group)) {
        println "Skipping ${origin} -> ${destination} : ${group}"
        return false
      }

      Path rootDir = project.directory.resolve(toDir)
      destination.licenses.each({ license ->
        Path licenseFile = rootDir.resolve("${destination.id.group.replace(".", "/")}/${destination.id.project}/${destination.version}/license-${license.identifier}.txt")
        if (Files.isRegularFile(licenseFile)) {
          return
        }

        if (Files.notExists(licenseFile.getParent())) {
          Files.createDirectories(licenseFile.getParent())
        }

        licenseFile << license.text
      })

      return true
    })
  }
}
