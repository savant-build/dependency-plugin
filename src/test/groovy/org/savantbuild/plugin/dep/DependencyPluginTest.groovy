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

import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.ArtifactMetaData
import org.savantbuild.dep.domain.Dependencies
import org.savantbuild.dep.domain.DependencyGroup
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Publication
import org.savantbuild.dep.domain.ReifiedArtifact
import org.savantbuild.dep.workflow.FetchWorkflow
import org.savantbuild.dep.workflow.PublishWorkflow
import org.savantbuild.dep.workflow.Workflow
import org.savantbuild.dep.workflow.process.CacheProcess
import org.savantbuild.dep.workflow.process.URLProcess
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertNull
import static org.testng.Assert.assertTrue
import static org.testng.Assert.fail

/**
 * Tests the groovy plugin.
 *
 * @author Brian Pontarelli
 */
class DependencyPluginTest {
  public static Path projectDir

  Output output

  Project project

  Path cacheDir

  @BeforeSuite
  static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../dependency-plugin")
    }
  }

  @Test
  void analyzeLicenses() {
    FileTools.prune(projectDir.resolve("build/test/licenses"))

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    try {
      plugin.analyzeLicenses([:])
      fail("Expected the analyze to throw an exception")
    } catch (Exception e) {
      // Expected
    }

    try {
      plugin.analyzeLicenses(null)
      fail("Expected the analyze to throw an exception")
    } catch (Exception e) {
      // Expected
    }

    try {
      plugin.analyzeLicenses(invalidLicenses: ["GPL-2.0-only"])
      fail("Expected the analyze to throw an exception")
    } catch (Exception e) {
      // Expected
    }

    plugin.analyzeLicenses(invalidLicenses: ["GPL-2.0-only"], ignoredIDs: ["org.savantbuild.test:leaf:*:*"])
  }

  @BeforeMethod
  void beforeMethod() {
    output = new SystemOutOutput(true)
//    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "dependency-plugin-test"
    project.version = new Version("1.0")
    project.licenses.add(License.parse("Apache-2.0", null))

    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Artifact("org.savantbuild.test:multiple-versions:1.0.0"),
            new Artifact("org.savantbuild.test:multiple-versions-different-dependencies:1.0.0")
        ),
        new DependencyGroup("runtime", true,
            new Artifact("org.savantbuild.test:intermediate:1.0.0")
        )
    )

    cacheDir = projectDir.resolve("../savant-dependency-management/test-deps/savant")

    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, cacheDir.toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, cacheDir.toString())
        ),
        output
    )
  }

  @Test
  void classpathWithNoDependencies() throws Exception {
    project.dependencies = null

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
    }

    assertEquals(classpath.toString(), "")
  }

  @Test
  void classpathWithDependencies() throws Exception {
    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
    }

    assertEquals(classpath.toString(),
        "${cacheDir.resolve("org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf3/1.0.0/leaf3-1.0.0.jar").toAbsolutePath()}"
    )
  }

  @Test
  void classpathWithPath() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/licenses"))

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
      path(location: "foo.jar")
    }

    assertEquals(classpath.toString(),
        "${cacheDir.resolve("org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar").toAbsolutePath()}:" +
            "${cacheDir.resolve("org/savantbuild/test/leaf3/1.0.0/leaf3-1.0.0.jar").toAbsolutePath()}:" +
            project.directory.resolve("foo.jar").toAbsolutePath()
    )
  }

  @Test
  void copy() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/copy"))

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.copy(to: "build/test/copy") {
      dependencies(group: "compile", transitive: true)
    }

    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/multiple-versions-1.1.0.jar")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/leaf1-1.0.0.jar")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/integration-build-2.1.1-{integration}.jar")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/multiple-versions-different-dependencies-1.1.0.jar")))
    assertTrue(Files.isRegularFile(projectDir.resolve("build/test/copy/leaf2-1.0.0.jar")))
  }

  @Test
  void integrate() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/integration"))

    project.publications.add("main",
        new Publication(new ReifiedArtifact("group:name:name:1.1.1:jar", [License.parse("BSD_2_Clause", null)]),
            new ArtifactMetaData(null, [License.parse("BSD_2_Clause", null)]), projectDir.resolve("LICENSE"), projectDir.resolve("README.md"))
    )
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, cacheDir.toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, projectDir.resolve("build/test/integration").toString())
        ),
        output
    )

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.integrate()

    Path integrationFile = projectDir.resolve("build/test/integration/group/name/1.1.1-{integration}/name-1.1.1-{integration}.jar")
    Path integrationSourceFile = projectDir.resolve("build/test/integration/group/name/1.1.1-{integration}/name-1.1.1-{integration}-src.jar")
    assertTrue(Files.isRegularFile(integrationFile))
    assertTrue(Files.isRegularFile(integrationSourceFile))
    assertEquals(Files.readAllBytes(integrationFile), Files.readAllBytes(projectDir.resolve("LICENSE")))
    assertEquals(Files.readAllBytes(integrationSourceFile), Files.readAllBytes(projectDir.resolve("README.md")))
  }

  @Test(enabled = true)
  void listUnusedDependencies() {
    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Artifact("org.savantbuild:savant-core:0.4.4"),
            new Artifact("org.apache.commons:commons-compress:1.7"),
        ),
        new DependencyGroup("test-compile", true,
            new Artifact("org.testng:testng:6.8.7"),
            new Artifact("org.apache.commons:commons-compress:1.7")
        )
    )
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, null),
            new CacheProcess(output, cacheDir.toString()),
            new URLProcess(output, "https://repository.savantbuild.org", null, null)
        ),
        new PublishWorkflow(
            new CacheProcess(output, null)
        ),
        output
    )
//    output.enableDebug()

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.listUnusedDependencies()
  }

  @Test
  void path() throws Exception {
    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Path path = plugin.path(id: "org.savantbuild.test:intermediate:1.0.0", group: "runtime")
    assertEquals(path, cacheDir.resolve("org/savantbuild/test/intermediate/1.0.0/intermediate-1.0.0.jar").toAbsolutePath())

    path = plugin.path(id: "org.savantbuild.test:bad:1.0.0", group: "runtime")
    assertNull(path)
  }

  @Test
  void printFull() throws Exception {
    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.printFull()
  }

  @Test
  void writeLicenses() {
    FileTools.prune(projectDir.resolve("build/test/licenses"))

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.writeLicenses(to: "build/test/licenses")

    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/multiple-versions/1.1.0/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf/1.0.0/license-GPL-2.0-only.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/integration-build/2.1.1-{integration}/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf1/1.0.0/license-Commercial.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf2/1.0.0/license-OtherNonDistributableOpenSource.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf3/1.0.0/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/intermediate/1.0.0/license-Apache-2.0.txt")))
  }

  @Test
  void writeLicensesWithGroups() {
    FileTools.prune(projectDir.resolve("build/test/licenses"))

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.writeLicenses(to: "build/test/licenses", groups: ["compile"])

    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/multiple-versions/1.1.0/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf/1.0.0/license-GPL-2.0-only.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/integration-build/2.1.1-{integration}/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/license-Apache-2.0.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf1/1.0.0/license-Commercial.txt")))
    assertTrue(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf2/1.0.0/license-OtherNonDistributableOpenSource.txt")))
    assertFalse(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/leaf3/1.0.0/license-Apache-2.0.txt")))
    assertFalse(Files.isRegularFile(project.directory.resolve("build/test/licenses/org/savantbuild/test/intermediate/1.0.0/license-Apache-2.0.txt")))
  }
}
