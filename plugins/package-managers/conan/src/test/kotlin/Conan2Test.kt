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

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

class Conan2Test : WordSpec({
    "parsePackages()" should {
        "parse single node" {
            val jsonString = """
            {
                "graph": {
                    "nodes": {
                        "0": {
                            "ref": "libcurl/7.85.0#d671ff2c55730f4b068bb66853c35bfc",
                            "id": "1",
                            "name": "libcurl",
                            "label": "libcurl/7.85.0",
                            "recipe_folder": "/home/user/.conan2/p/libcu4a86ede08c3bd/e",
                            "dependencies": {
                                "2": {
                                    "ref": "openssl/3.2.2",
                                    "run": false,
                                    "libs": true,
                                    "skip": false,
                                    "test": false,
                                    "force": false,
                                    "direct": true,
                                    "build": false,
                                    "transitive_headers": null,
                                    "transitive_libs": null,
                                    "headers": true,
                                    "package_id_mode": "minor_mode",
                                    "visible": true
                                },
                            },
                        }
                    },
                },
            }
            """.trimIndent()

            val conanGraph = parseConanGraph(jsonString)

            conanGraph.nodes.keys shouldBe setOf("0")
            val nodeInfo = conanGraph.nodes.getValue("0")
            nodeInfo.ref shouldBe "libcurl/7.85.0#d671ff2c55730f4b068bb66853c35bfc"
            nodeInfo.id shouldBe "1"
            nodeInfo.name shouldBe "libcurl"
            nodeInfo.label shouldBe "libcurl/7.85.0"
            nodeInfo.recipeFolder shouldBe "/home/user/.conan2/p/libcu4a86ede08c3bd/e"
            nodeInfo.dependencies.keys shouldBe setOf("2")
            val dependencyNode = nodeInfo.dependencies.getValue("2")
            dependencyNode.ref shouldBe "openssl/3.2.2"
            dependencyNode.direct shouldBe true
            dependencyNode.build shouldBe false
        }
    }
})
