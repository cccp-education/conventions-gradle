import org.gradle.plugin.compatibility.compatibility
import org.gradle.plugins.signing.SigningExtension

plugins {
    kotlin("jvm") version libs.versions.kotlin
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version libs.versions.plugin.publish
}

group = "education.cccp.build"

// CNV-16 — JitPack derives the published version from the git tag.
// The root `jitpack.yml` passes -Pversion="${VERSION#v}"; normal local and CI
// builds keep the frozen release version below. A JitPack build without the
// property is refused so the served artefact can never diverge from its tag.
val jitpackBuild = providers.environmentVariable("JITPACK").orNull == "true"
val versionProperty = providers.gradleProperty("version")
if (jitpackBuild && !versionProperty.isPresent) {
    error(
        "JitPack build requires -Pversion=\"\${VERSION#v}\" (see jitpack.yml). " +
            "Refusing to publish an artefact whose version does not match the git tag.",
    )
}
version = versionProperty.getOrElse("0.0.15")

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val functionalTest by sourceSets.creating {
    java.srcDir("src/functionalTest/kotlin")
    resources.srcDir("src/functionalTest/resources")
}

sourceSets.test {
    java.srcDir("src/test/scenarios")
}

// ── Single source of truth for the workspace-bom fallback version ───────────
// The version lives in gradle/libs.versions.toml ([versions].workspace-bom) and
// is materialised as a resource so TestDependencies can read it at consumer
// runtime, even without a local `libs` catalog. This replaces the hardcoded
// constant that had drifted 45 versions behind (0.0.13 vs 0.0.58, S-019 P2-A).
val generateWorkspaceBomInfo =
    tasks.register("generateWorkspaceBomInfo") {
        val bomVersion = libs.versions.workspace.bom.get()
        val outputDir = layout.buildDirectory.dir("generated/build-info")
        inputs.property("workspaceBomVersion", bomVersion)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("build/workspace-bom.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("version=$bomVersion\n")
        }
    }

sourceSets.main {
    resources.srcDir(generateWorkspaceBomInfo)
}

// ── Single source of truth for the workspace Gradle wrapper version ────────
// The version lives in gradle/libs.versions.toml ([versions].gradle) and is
// materialised as a resource so GradleWrapperVersion can read it at consumer
// runtime. `verifyGradleWrapper` (wired to `check`) then fails any CI whose
// wrapper drifts from it.
val generateGradleVersionInfo =
    tasks.register("generateGradleVersionInfo") {
        val gradleVersion = libs.versions.gradle.get()
        val outputDir = layout.buildDirectory.dir("generated/gradle-info")
        inputs.property("gradleVersion", gradleVersion)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("build/gradle-version.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("version=$gradleVersion\n")
        }
    }

sourceSets.main {
    resources.srcDir(generateGradleVersionInfo)
}

// ── The conventions version itself, materialised for the adoption audit ────
// AdoptionAuditTask compares every consumer pin to this version, so the
// expected value can never drift from the published plugin.
val generateConventionsVersionInfo =
    tasks.register("generateConventionsVersionInfo") {
        val conventionsVersion = version.toString()
        val outputDir = layout.buildDirectory.dir("generated/conventions-info")
        inputs.property("conventionsVersion", conventionsVersion)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("build/conventions-version.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("version=$conventionsVersion\n")
        }
    }

sourceSets.main {
    resources.srcDir(generateConventionsVersionInfo)
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.2.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.8")
    implementation(libs.kotlin.gradle.plugin)
    // Gradle Plugin Portal compatibility metadata (ACADEMY-16 / S-027):
    // the API classes are needed to configure `PluginDeclaration.compatibility`
    // from the convention plugin (the consumer's plugin-publish 2.1+ applies
    // the plugin itself; this dependency only provides the compile/runtime types).
    implementation("org.gradle.plugin:compatibility-plugin:1.1.0")

    testImplementation(platform("com.github.cccp-education.workspace-bom:workspace-bom:0.0.136"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.java8)
    testImplementation(libs.junit.platform.suite)

    add(functionalTest.implementationConfigurationName, gradleTestKit())
    add(functionalTest.implementationConfigurationName, libs.kotlin.test.junit5)
    add(functionalTest.runtimeOnlyConfigurationName, platform("com.github.cccp-education.workspace-bom:workspace-bom:0.0.136"))
    add(functionalTest.runtimeOnlyConfigurationName, libs.junit.platform.launcher)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    useJUnitPlatform()
    // The published workspace-bom version is injected by Gradle (source of
    // truth = the version catalog) so the functional tests assert against the
    // resolved version instead of a hardcoded literal that can drift.
    systemProperty("conventions.workspaceBom.version", libs.versions.workspace.bom.get())
    // The plugin version is injected too, so the adoption-audit fixture can pin
    // the *current* version instead of a literal that drifts on every bump.
    systemProperty("conventions.version", version.toString())
}

gradlePlugin.testSourceSets.add(functionalTest)
tasks.check { dependsOn(functionalTestTask) }

val cucumberTest = tasks.register<Test>("cucumberTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = configurations.testRuntimeClasspath.get() +
        sourceSets.test.get().output +
        sourceSets.main.get().output
    useJUnitPlatform { excludeEngines("junit-jupiter") }
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
    shouldRunAfter("test")
}

tasks.named<Test>("test") {
    filter { excludeTestsMatching("*.scenarios.*") }
}

tasks.check { dependsOn(cucumberTest) }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

gradlePlugin {
    website.set("https://github.com/cccp-education/conventions-gradle")
    vcsUrl.set("https://github.com/cccp-education/conventions-gradle.git")
    plugins {
        register("gradlePlugin") {
            id = "education.cccp.build.gradle-plugin"
            displayName = "Gradle Plugin Conventions"
            description = "Applies java-gradle-plugin, kotlin-jvm, maven-publish, Java 24 target, withSourcesJar, withJavadocJar, JUnit test config"
            implementationClass = "build.GradlePluginConventionsPlugin"
            tags = listOf("cccp", "conventions", "gradle-plugin")
        }
        register("publishing") {
            id = "education.cccp.build.publishing"
            displayName = "Publishing Conventions"
            description = "Configures Maven Central POM (developers, scm, licenses), conditional signing, and relocation for all Maven publications"
            implementationClass = "build.PublishingConventionsPlugin"
            tags = listOf("cccp", "conventions", "publishing", "maven", "signing")
        }
        register("functionalTest") {
            id = "education.cccp.build.functional-test"
            displayName = "Functional Test Conventions"
            description = "Configures functionalTest source set with GradleTestKit, JUnit 5, AssertJ, task registration, and check.dependsOn"
            implementationClass = "build.FunctionalTestConventionsPlugin"
            tags = listOf("cccp", "conventions", "functional-test", "gradle-test-kit")
        }
        register("cucumber") {
            id = "education.cccp.build.cucumber"
            displayName = "Cucumber BDD Conventions"
            description = "Configures Cucumber BDD test source sets (features/scenarios), cucumberTest task with JUnit Platform exclusion, test filter, check.dependsOn, and optional multi-task CucumberTaskSpec support"
            implementationClass = "build.CucumberConventionsPlugin"
            tags = listOf("cccp", "conventions", "cucumber", "bdd")
        }
        register("library") {
            id = "education.cccp.build.library"
            displayName = "Library Conventions"
            description = "Configures N0 contract libraries: java-library, kotlin-jvm, Java 25, sources/javadoc jars, JUnit test config, workspace guards (no java-gradle-plugin, no publishing)"
            implementationClass = "build.LibraryConventionsPlugin"
            tags = listOf("cccp", "conventions", "library", "contracts", "n0")
        }
        register("logbackExclusion") {
            id = "education.cccp.build.logback-exclusion"
            displayName = "Logback Exclusion Conventions"
            description = "Excludes logback-classic from test classpaths to avoid conflicts with asciidoctor JRuby logging stack, while preserving slf4j-api"
            implementationClass = "build.LogbackExclusionConventionsPlugin"
            tags = listOf("cccp", "conventions", "logback", "asciidoctor")
        }
        register("lint") {
            id = "education.cccp.build.lint"
            displayName = "Lint Conventions"
            description = "Applies ktlint (Kotlin coding style) and detekt (code smells) with check.dependsOn(detekt)"
            implementationClass = "build.LintConventionsPlugin"
            tags = listOf("cccp", "conventions", "lint", "ktlint", "detekt")
        }
        register("kover") {
            id = "education.cccp.build.kover"
            displayName = "Kover Conventions"
            description = "Applies Kover code coverage with configurable threshold, includedSourceSets (main, functionalTest), and HTML/XML reports on check"
            implementationClass = "build.KoverConventionsPlugin"
            tags = listOf("cccp", "conventions", "kover", "coverage")
        }
        all {
            // Gradle Plugin Portal compatibility metadata (ACADEMY-16 / S-027).
            // Honest default: false until each convention plugin's consumer
            // build is proven with the feature enabled.
            compatibility {
                features {
                    configurationCache.set(false)
                    isolatedProjects.set(false)
                }
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Conventions Gradle Plugins")
                description.set("Centralized build convention plugins for education.cccp boroughs")
                url.set("https://github.com/cccp-education/conventions-gradle")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cccp-education")
                        name.set("CCCP Education")
                        email.set("cccp.edu@gmail.com")
                    }
                }
                scm {
                    connection.set("https://github.com/cccp-education/conventions-gradle.git")
                    developerConnection.set("https://github.com/cccp-education/conventions-gradle.git")
                    url.set("https://github.com/cccp-education/conventions-gradle")
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

// CNV-16 — the `signing` plugin is applied ONLY when a signature is expected.
// `com.gradle.plugin-publish` auto-signs every publication as soon as it sees
// the `signing` plugin (2.2.1, `enableSigning`), so applying it on JitPack
// (which has no GPG key) would fail `publishToMavenLocal`. Signing stays
// enabled for local releases and for CCCP_PUBLISH=true release jobs.
val isPublishing = System.getenv("CCCP_PUBLISH") == "true"
val isCi = System.getenv("CI") == "true"
if (!jitpackBuild && (isPublishing || !isCi) && !version.toString().endsWith("-SNAPSHOT")) {
    apply(plugin = "signing")
    configure<SigningExtension> {
        useGpgCmd()
        sign(publishing.publications)
    }
}
