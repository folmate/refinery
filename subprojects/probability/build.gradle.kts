/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Probabilistic analysis module"
}

dependencies {
	implementation(project(":refinery-store-dse"))
	implementation(project(":refinery-language-model"))
}
