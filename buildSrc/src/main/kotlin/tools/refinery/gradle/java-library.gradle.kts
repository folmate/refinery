/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	id("tools.refinery.gradle.internal.java-basic-library")
	id("tools.refinery.gradle.java-conventions")
}

val libs = the<LibrariesForLibs>()

dependencies {
	testRuntimeOnly(libs.slf4j.simple)
}
