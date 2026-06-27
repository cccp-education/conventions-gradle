plugins {
    kotlin("jvm") version libs.versions.kotlin
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version libs.versions.plugin.publish
}

group = "education.cccp.build"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(24)
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

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlin.gradle.plugin)

    testImplementation(platform("education.cccp:workspace-bom:0.0.4"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.cucumber.java8)
    testImplementation(libs.junit.platform.suite)

    add(functionalTest.implementationConfigurationName, gradleTestKit())
    add(functionalTest.implementationConfigurationName, libs.kotlin.test.junit5)
    add(functionalTest.runtimeOnlyConfigurationName, platform("education.cccp:workspace-bom:0.0.4"))
    add(functionalTest.runtimeOnlyConfigurationName, libs.junit.platform.launcher)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTest)
tasks.check { dependsOn(functionalTestTask) }

val cucumberTest = tasks.register<Test>("cucumberTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = configurations.testRuntimeClasspath.get() +
        sourceSets.test.get().output +
        sourceSets.main.get().output +
        files(tasks.jar.get().archiveFile)
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
            implementationClass = "education.cccp.build.GradlePluginConventionsPlugin"
            tags = listOf("cccp", "conventions", "gradle-plugin")
        }
        register("publishing") {
            id = "education.cccp.build.publishing"
            displayName = "Publishing Conventions"
            description = "Configures Maven Central POM (developers, scm, licenses), conditional signing, and relocation for all Maven publications"
            implementationClass = "education.cccp.build.PublishingConventionsPlugin"
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
            implementationClass = "education.cccp.build.CucumberConventionsPlugin"
            tags = listOf("cccp", "conventions", "cucumber", "bdd")
        }
    }
}

publishing {
    repositories {
        mavenCentral()
    }
}
