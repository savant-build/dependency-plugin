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

import org.savantbuild.domain.Project
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.runtime.BuildFailureException

import static org.savantbuild.dep.DependencyService.ResolveConfiguration
import static org.savantbuild.dep.DependencyService.ResolveConfiguration.TypeResolveConfiguration

/**
 * Base class for delegates that might work on dependencies.
 *
 * @author Brian Pontarelli
 */
class BaseDependencyDelegate {
  public static final String ERROR_MESSAGE = "The dependencies method must be called like this:\n\n" +
      "  dependencies(group: \"compile\", transitive: true, fetchSource: true, transitiveGroups: [\"compile\", \"runtime\"])"

  public static final Map<String, Class<? extends Object>> DEPENDENCIES_ATTRIBUTE_TYPES =
      ["group": String.class, "transitive": Boolean.class, "fetchSource": Boolean.class, "transitiveGroups": List.class]

  public static final Map<String, Object> DEPENDENCIES_DEFAULT_ATTRIBUTES =
      ["transitive": true, "fetchSource": false, "transitiveGroups": []]


  public Project project

  public ResolveConfiguration resolveConfiguration

  BaseDependencyDelegate(project) {
    this.project = project
  }

  /**
   * Adds a set of dependencies to fetch as part of a classpath, copy, etc. This takes a set of attributes for the
   * dependencies like this:
   *
   * <pre>
   *   dependencies(group: "compile", transitive: true, fetchSource: false, transitiveGroups: ["compile", "runtime"])
   * </pre>
   *
   * @param attributes The attributes.
   * @return The ResolveConfiguration that this dependencies set is added to.
   */
  ResolveConfiguration dependencies(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["group", "transitive", "fetchSource", "transitiveGroups"], ["group"], DEPENDENCIES_ATTRIBUTE_TYPES)) {
      throw new BuildFailureException(ERROR_MESSAGE)
    }

    GroovyTools.putDefaults(attributes, DEPENDENCIES_DEFAULT_ATTRIBUTES)

    def group = attributes["group"]
    def transitive = attributes["transitive"]
    def fetchSource = attributes["fetchSource"]
    def transitiveGroups = attributes["transitiveGroups"]

    if (resolveConfiguration == null) {
      resolveConfiguration = new ResolveConfiguration()
    }

    resolveConfiguration.with(group, new TypeResolveConfiguration(fetchSource, transitive, transitiveGroups))
  }
}
