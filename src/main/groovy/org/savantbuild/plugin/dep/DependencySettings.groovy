/*
 * Copyright (c) 2024, Inversoft Inc., All Rights Reserved
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

import org.savantbuild.dep.domain.License

/**
 * Settings for the Dependency plugin.
 *
 * @author Brian Pontarelli
 */
class DependencySettings {
  LicenseSettings license = new LicenseSettings()

  class LicenseSettings {
    /**
     * List of allowed license prefixes for the project. The default includes the most common licenses that are not
     * copyleft in any way.
     */
    List<String> allowedPrefixes = ["AFL-", "Apache-", "Artistic-", "BSD-", "ECL-", "EDL-", "EFL-", "EPL-", "LGPL-", "LPL-", "LPPL-", "MIT-", "MPL-", "OLDAP-", "PDDL-", "PHP-", "Python-", "SGI-", "UPL-", "W3C-", "ZPL-"]

    /**
     * List of allowed license ids (SPDX identifiers). The default includes the most common licenses that are now copyleft in
     * any way.
     */
    List<String> allowedIDs = ["GPL-2.0-with-classpath-exception", "ICU", "JSON", "MIT", "PostgreSQL", "Ruby", "Vim", "W3C", "X11", "Zlib"]

    /**
     * List of license objects that are allowed. This is a fall back for dependencies pulled from Maven that do not use SPDX.
     */
    List<License> allowedLicenses = []

    /**
     * List of artifact ids (wildcards are allowed) that are ignored when performing license analysis.
     */
    List<String> ignoredArtifactIDs = []
  }
}
