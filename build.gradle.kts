plugins {
    kotlin("jvm") version "2.3.20"
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
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

// ── Functional Test source set (GradleTestKit) ──
val functionalTest by sourceSets.creating {
    java.srcDir("src/functionalTest/kotlin")
    resources.srcDir("src/functionalTest/resources")
}

// ── Cucumber test source dirs ──
sourceSets.test {
    java.srcDir("src/test/scenarios")
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin", "2.3.20"))

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(platform("io.cucumber:cucumber-bom:7.21.0"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-java8")
    testImplementation("org.junit.platform:junit-platform-suite")

    add(functionalTest.implementationConfigurationName, gradleTestKit())
    add(functionalTest.implementationConfigurationName, kotlin("test-junit5"))
    add(functionalTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher")
}

// ── Functional Test task ──
val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTest)
tasks.check { dependsOn(functionalTestTask) }

// ── Cucumber test task ──
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

// ── Common test config ──
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// ── Plugin metadata ──
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
    }
}

publishing {
    repositories {
        mavenCentral()
    }
}
