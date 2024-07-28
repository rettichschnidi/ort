/*
 * Copyright (C) 2024 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.packagemanagers.conan

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromJsonElement
import org.ossreviewtoolkit.analyzer.AbstractPackageManagerFactory
import org.ossreviewtoolkit.analyzer.PackageManager
import org.ossreviewtoolkit.downloader.VcsHost
import org.ossreviewtoolkit.downloader.VersionControlSystem
import org.ossreviewtoolkit.model.*
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.utils.common.CommandLineTool
import org.ossreviewtoolkit.utils.common.fieldNamesOrEmpty
import org.semver4j.RangesList
import org.semver4j.RangesListFactory
import java.io.File

/**
 * The [Conan](https://conan.io/) package manager for C / C++, version 2.x.
 *
 * This package manager supports the following [options][PackageManagerConfiguration.options]:
 * - *lockfileName*: The name of the lockfile, which is used for analysis if allowDynamicVersions is set to false.
 *   The lockfile should be located in the analysis root. Currently only one lockfile is supported per Conan project.
 */
class Conan2(
    name: String,
    analysisRoot: File,
    analyzerConfig: AnalyzerConfiguration,
    repoConfig: RepositoryConfiguration
) : PackageManager(name, analysisRoot, analyzerConfig, repoConfig), CommandLineTool {
    companion object {
        /**
         * The name of the option to specify the name of the lockfile.
         */
        const val OPTION_LOCKFILE_NAME = "lockfileName"

        private val DUMMY_COMPILER_SETTINGS = arrayOf(
            "-s", "compiler=gcc",
            "-s", "compiler.libcxx=libstdc++",
            "-s", "compiler.version=11.1"
        )
    }

    class Factory : AbstractPackageManagerFactory<Conan2>("Conan2") {
        override val globsForDefinitionFiles = listOf("conanfile*.txt", "conanfile*.py")

        override fun create(
            analysisRoot: File,
            analyzerConfig: AnalyzerConfiguration,
            repoConfig: RepositoryConfiguration
        ): Conan2 = Conan2(type, analysisRoot, analyzerConfig, repoConfig)
    }

    override fun resolveDependencies(definitionFile: File, labels: Map<String, String>): List<ProjectAnalyzerResult> {
        val workingDir = definitionFile.parentFile
        val conanGraphInfoResult = when (val lockfileName = options[Conan.OPTION_LOCKFILE_NAME]) {
            null -> run(workingDir, "graph", "info", definitionFile.name, "--format", "json", *DUMMY_COMPILER_SETTINGS)
            else -> run(workingDir, "graph", "info", definitionFile.name, "-l", lockfileName, "--format", "json")
        }
        val conanGraph = parseConanGraph(conanGraphInfoResult.stdout)
        assert(conanGraph.root.keys == setOf("0"))
        val projectPackage = conanGraph.nodes.getValue("0")

        return listOf(
            ProjectAnalyzerResult(
                project = Project(
                    id = Identifier(
                        type = managerName,
                        namespace = "",
                        name = projectPackage.name.orEmpty(),
                        version = projectPackage.version.orEmpty()
                    ),
                    definitionFilePath = VersionControlSystem.getPathInfo(definitionFile).path,
                    authors = setOf(projectPackage.author.orEmpty()),
                    declaredLicenses = projectPackage.license.toSet(),
                    vcs = VcsHost.parseUrl(projectPackage.url.orEmpty()),
                    homepageUrl = projectPackage.homepage.orEmpty(),
                ),
                packages = setOf()
            )
        )
    }

    override fun command(workingDir: File?) = "conan"

    // Conan could report version strings like:
    // Conan version 2.5.0
    override fun transformVersion(output: String) = output.removePrefix("Conan version ")

    override fun getVersionRequirement(): RangesList = RangesListFactory.create(">=2.5")
}

private val JSON = Json {
    ignoreUnknownKeys = true
    allowTrailingComma = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

internal fun parseConanGraph(s: String): ConanGraph {
    val topLevel: Map<String, ConanGraph> = JSON.decodeFromString(s)
    assert(topLevel.keys == setOf("graph"))
    return topLevel.getValue("graph")
}

@Serializable
internal data class ConanGraphDependency(
    val ref: String,
    val direct: Boolean,
    val build: Boolean,
)

@Serializable
internal data class ConanGraphNode(
    val ref: String,
    val id: String,
    val license: List<String> = emptyList(),
    val author: String? = null,
    val homepage: String? = null,
    val version: String? = null,
    val url: String? = null,
    val name: String? = null,
    val label: String,
    val recipeFolder: String,
    val dependencies: Map<String, ConanGraphDependency> = emptyMap(),
)

@Serializable
internal data class ConanGraph(
    val nodes: Map<String, ConanGraphNode> = emptyMap(),
    val root: Map<String, String> = emptyMap(),
)
