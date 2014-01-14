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

  DependencyPlugin plugin

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
    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "dependency-plugin-test"
    project.version = new Version("1.0")
    project.license = License.Apachev2

    project.dependencies = new Dependencies(
        new DependencyGroup("compile", true,
            new Dependency("org.savantbuild.test:multiple-versions:1.0.0", false),
            new Dependency("org.savantbuild.test:multiple-versions-different-dependencies:1.0.0", false)
        ),
        new DependencyGroup("run", true,
            new Dependency("org.savantbuild.test:intermediate:1.0.0", false)
        )
    );
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        ),
        new PublishWorkflow(
            new CacheProcess(output, projectDir.resolve("src/test/repository").toString())
        )
    )

    plugin = new DependencyPlugin(project, output)
  }

  @Test
  public void classpathNoClosure() throws Exception {
    Classpath classpath = plugin.classpath(new DependencyService.ResolveConfiguration()
        .with("compile", new DependencyService.ResolveConfiguration.TypeResolveConfiguration(true, true))
    )

    assertEquals(classpath.toString(),
        "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar")}"
    )
  }

  @Test
  public void classpathWithClosure() throws Exception {
    Classpath classpath = plugin.classpath(new DependencyService.ResolveConfiguration()
        .with("compile", new DependencyService.ResolveConfiguration.TypeResolveConfiguration(true, true))
    ) {
      path "foo.jar"
    }

    assertEquals(classpath.toString(),
        "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions/1.1.0/multiple-versions-1.1.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf/1.0.0/leaf1-1.0.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/integration-build/2.1.1-{integration}/integration-build-2.1.1-{integration}.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/multiple-versions-different-dependencies/1.1.0/multiple-versions-different-dependencies-1.1.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf1/1.0.0/leaf1-1.0.0.jar")}:" +
            "${projectDir.resolve("src/test/repository/org/savantbuild/test/leaf2/1.0.0/leaf2-1.0.0.jar")}:" +
            "foo.jar"
    )
  }

  @Test
  public void integrate() throws Exception {
    FileTools.prune(projectDir.resolve("build/test/integration"));

    project.publications.add("main",
        new Publication(new Artifact("group:name:name:1.1.1:jar", License.BSD),
            new ArtifactMetaData(null, License.BSD), projectDir.resolve("LICENSE"), projectDir.resolve("README.md"))
    );
    project.workflow = new Workflow(
        new FetchWorkflow(output),
        new PublishWorkflow(new CacheProcess(output, projectDir.resolve("build/test/integration").toString()))
    );

    plugin.integrate();

    Path integrationFile = projectDir.resolve("build/test/integration/group/name/1.1.1-{integration}/name-1.1.1-{integration}.jar");
    Path integrationSourceFile = projectDir.resolve("build/test/integration/group/name/1.1.1-{integration}/name-1.1.1-{integration}-src.jar");
    assertTrue(Files.isRegularFile(integrationFile));
    assertTrue(Files.isRegularFile(integrationSourceFile));
    assertEquals(Files.readAllBytes(integrationFile), Files.readAllBytes(projectDir.resolve("LICENSE")));
    assertEquals(Files.readAllBytes(integrationSourceFile), Files.readAllBytes(projectDir.resolve("README.md")));
  }
}
