/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Integration to EMF"
}

dependencies {
	implementation("org.eclipse.emf:org.eclipse.emf.ecore:2.38.0")
}
