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

import org.objectweb.asm.ClassReader
import org.savantbuild.dep.domain.ResolvedArtifact
import org.savantbuild.dep.graph.ResolvedArtifactGraph
import org.savantbuild.domain.Project
import org.savantbuild.output.Output

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

/**
 *
 * @author Brian Pontarelli
 */
class DependencyChecker {
  private final Output output

  private final DependencyPlugin plugin

  private final Project project


  DependencyChecker(Project project, Output output, DependencyPlugin plugin) {
    this.output = output
    this.project = project
    this.plugin = plugin
  }

  Set<ResolvedArtifact> check(Path buildDir, String dependencyGroup) {
    Set<String> projectClasses = new HashSet<>()
    Path mainBuildDir = project.directory.resolve(buildDir)
    if (Files.notExists(mainBuildDir)) {
      fail("Missing build output directory [${buildDir}]")
    }

    Files.walk(mainBuildDir).forEach({ path ->
      if (path.toString().endsWith(".class")) {
        ImportClassVisitor importClassVisitor = new ImportClassVisitor()
        new ClassReader(Files.readAllBytes(path)).accept(importClassVisitor, ClassReader.SKIP_FRAMES)
        projectClasses.addAll(importClassVisitor.classes)
      }
    })

    output.debug("Classes that the project uses are %s", projectClasses)

    ResolvedArtifactGraph resolvedArtifactGraph = plugin.resolve() {
      dependencies(group: dependencyGroup, transitive: false, fetchSource: false)
    }

    Set<ResolvedArtifact> unused = new HashSet<>()
    if (resolvedArtifactGraph.size() == 0) {
      return unused
    }

    resolvedArtifactGraph.traverse(resolvedArtifactGraph.root, true, {origin, destination, edgeValue, depth ->
      output.debug("Checking compile dependency [%s] at [%s]", destination, destination.file)

      Set<String> dependencyClasses = new HashSet<>()
      JarFile jarFile = new JarFile(destination.file.toFile())
      jarFile.entries().each { entry ->
        if (entry.name.endsWith(".class")) {
          output.debug("Handling JAR file entry [%s]", entry.name)
          dependencyClasses.add(entry.name.substring(0, entry.name.length() - 6).replace(".", "/"))
        }
      }
      jarFile.close()

      output.debug("Classes that the dependency [%s] provides are %s", destination, dependencyClasses)
      if (!dependencyClasses.removeAll(projectClasses)) {
        output.info("Unused dependency [%s]", destination)
        unused.add(destination)
      }

      return false
    })

    return unused
  }
}
