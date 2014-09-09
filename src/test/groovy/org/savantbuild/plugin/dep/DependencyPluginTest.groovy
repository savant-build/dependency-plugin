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

import org.savantbuild.dep.domain.*
import org.savantbuild.dep.workflow.FetchWorkflow
import org.savantbuild.dep.workflow.PublishWorkflow
import org.savantbuild.dep.workflow.Workflow
import org.savantbuild.dep.workflow.process.CacheProcess
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.lang.Classpath
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue

/**
 * Tests the groovy plugin.
 *
 * @author Brian Pontarelli
 */
class DependencyPluginTest {
  public static Path projectDir

  Output output

  Project project

  @BeforeSuite
  public static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../dependency-plugin")
    }
  }

  @BeforeMethod
  public void beforeMethod() {
    output = new SystemOutOutput(true)
//    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "dependency-plugin-test"
    project.version = new Version("1.0")
    project.license = License.Apachev2

    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Artifact("org.savantbuild.test:multiple-versions:1.0.0"),
            new Artifact("org.savantbuild.test:multiple-versions-different-dependencies:1.0.0")
        ),
        new DependencyGroup("runtime", true,
            new Artifact("org.savantbuild.test:intermediate:1.0.0")
        )
    )
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        )
    )
  }

  @Test
  public void classpathWithNoDependencies() throws Exception {
    project.dependencies = null

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
    }

    assertEquals(classpath.toString(), "")
  }

  @Test
  public void classpathWithDependencies() throws Exception {
    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
    }

    assertEquals(classpath.toString(),
        "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar").toAbsolutePath()}"
    )
  }

  @Test
  public void classpathWithPath() throws Exception {
    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    Classpath classpath = plugin.classpath {
      dependencies(group: "compile", transitive: true, fetchSource: true)
      path(location: "foo.jar")
    }

    assertEquals(classpath.toString(),
        "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar").toAbsolutePath()}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar").toAbsolutePath()}:" +
            project.directory.resolve("foo.jar").toAbsolutePath()
    )
  }

  @Test
  public void copy() throws Exception {
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
  public void integrate() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/integration"))

    project.publications.add("main",
        new Publication(new ReifiedArtifact("group:name:name:1.1.1:jar", License.BSD),
            new ArtifactMetaData(null, License.BSD), projectDir.resolve("LICENSE"), projectDir.resolve("README.md"))
    )
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        ),
        new PublishWorkflow(new CacheProcess(output, projectDir.resolve("build/test/integration").toString()))
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
  public void listUnusedDependencies() {
    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Artifact("org.savantbuild:savant-core:0.2.0-{integration}"),
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
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, null)
        )
    )
//    output.enableDebug()

    DependencyPlugin plugin = new DependencyPlugin(project, new RuntimeConfiguration(), output)
    plugin.listUnusedDependencies()
  }
}
