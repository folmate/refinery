/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarnTaskType
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

frontend {
	assembleScript.set("run build")
}

val viteOutputDir = layout.buildDirectory.dir("vite")

val productionResources: Provider<Directory> = viteOutputDir.map { it.dir("production") }

val productionAssets: Configuration by configurations.creating {
	isCanBeConsumed = true
	isCanBeResolved = false
}

val sourcesWithoutTypes: FileCollection = fileTree("src") {
	exclude("**/*.typegen.ts")
}

val sourcesWithTypes: FileCollection = fileTree("src") + fileTree("types")

val installationState: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
)

val assembleConfigFiles: FileCollection = installationState + files(
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"tsconfig.node.json",
	"tsconfig.shared.json",
	"vite.config.ts",
) + fileTree("config")

val assembleSources: FileCollection = sourcesWithTypes + fileTree("public") + files("index.html")

val assembleFiles: FileCollection = assembleSources + assembleConfigFiles

val lintingFiles: FileCollection = sourcesWithTypes + assembleConfigFiles + files(
	rootProject.file(".eslintrc.cjs"),
	rootProject.file("prettier.config.cjs"),
)

tasks {
	val generateXStateTypes by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		inputs.files(sourcesWithoutTypes)
		inputs.files(installationState)
		outputs.dir("src")
		args.set("run typegen")
		description = "Generate TypeScript typings for XState state machines."
	}

	assembleFrontend {
		dependsOn(rootProject.project("refinery-client-js").tasks.named("assembleFrontend"))
		dependsOn(generateXStateTypes)
		inputs.files(assembleFiles)
		outputs.dir(productionResources)
	}

	val typeCheckFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(rootProject.project("refinery-client-js").tasks.named("typeCheckFrontend"))
		dependsOn(generateXStateTypes)
		inputs.files(lintingFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		args.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarnTaskType::class) {
		dependsOn(installFrontend)
		dependsOn(generateXStateTypes)
		dependsOn(typeCheckFrontend)
		inputs.files(lintingFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		args.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarnTaskType>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(generateXStateTypes)
		dependsOn(typeCheckFrontend)
		inputs.files(lintingFiles)
		args.set("run lint:fix")
		group = "verification"
		description = "Fix TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	clean {
		delete("dev-dist")
		delete(fileTree("src") {
			include("**/*.typegen.ts")
		})
	}
}

artifacts {
	add("productionAssets", productionResources) {
		builtBy(tasks.assembleFrontend)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", "src")
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
