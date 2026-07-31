Feature: Gradle Plugin Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the conventions plugin to my project
  So that my build is standardised with java-gradle-plugin, kotlin-jvm, and JUnit test config

  Scenario: Plugin applies required plugins
    Given a project applies the conventions plugin
    Then the project has the java-gradle-plugin applied
    And the project has the kotlin-jvm plugin applied
    And the project has the maven-publish plugin applied

  Scenario: Plugin configures Java compatibility
    Given a project applies the conventions plugin
    Then the project uses Java 25 source compatibility
    And the project uses Java 25 target compatibility
    And the project has sources jar task
    And the project has javadoc jar task

  Scenario: Plugin configures test tasks
    Given a project applies the conventions plugin
    Then test tasks use JUnit Platform
    And test logging shows passed, skipped, and failed events

  # CNV-7.2 — Plugin ajoute les dépendances junit test à testImplementation/testRuntimeOnly
  Scenario: Plugin adds junit test dependencies
    Given a project applies the conventions plugin with dependency inspection
    Then the testImplementation configuration contains kotlin-test-junit5
    And the testImplementation configuration contains junit-jupiter
    And the testImplementation configuration contains junit-platform-params
    And the testImplementation configuration contains assertj-core
    And the testRuntimeOnly configuration contains junit-platform-launcher
    And the gradle testImplementation configuration has the workspace-bom platform

  # CNV-10.1 — configureRepositories
  Scenario: Plugin configures repositories
    Given a project applies the conventions plugin
    Then the project has mavenLocal repository configured
    And the project has mavenCentral repository configured
    And the project has gradlePluginPortal repository configured

  # CNV-10.1 — configureBuildCache
  Scenario: Plugin enables build cache
    Given a project applies the conventions plugin
    Then the build cache is enabled

  # CNV-10.7 — TestDependencies fallback hardcoded (no catalog)
  Scenario: Plugin adds junit test dependencies without version catalog
    Given a project applies the conventions plugin without version catalog
    Then the testImplementation configuration contains junit-jupiter from fallback

  # CNV-11.1 — GradlePluginConventionsExtension defaults
  Scenario: Extension has default values
    Given a project applies the conventions plugin
    Then the gradlePluginConventions extension has enableDynamicAgentLoading default true
    And the gradlePluginConventions extension has maxHeapSize default null
    And the gradlePluginConventions extension has parallelExecution default false

  # CNV-11.1 — GradlePluginConventionsExtension override
  Scenario: Extension values can be overridden via DSL
    Given a project applies the conventions plugin with custom extension values
    Then the gradlePluginConventions extension has enableDynamicAgentLoading set to false
    And the gradlePluginConventions extension has maxHeapSize set to "2g"
    And the gradlePluginConventions extension has parallelExecution set to true

  # CNV-11.2 — configureTestTasks enriched with JVM options
  Scenario: Test tasks receive jvmArgs when enableDynamicAgentLoading is true
    Given a project applies the conventions plugin
    Then the build succeeds with default extension

  Scenario: Test tasks do not receive jvmArgs when enableDynamicAgentLoading is false
    Given a project applies the conventions plugin with enableDynamicAgentLoading false
    Then the build succeeds with enableDynamicAgentLoading false

  Scenario: Test tasks receive maxHeapSize when configured
    Given a project applies the conventions plugin with maxHeapSize "4g"
    Then the build succeeds with maxHeapSize "4g"

  Scenario: Test tasks receive parallel execution system property when enabled
    Given a project applies the conventions plugin with parallelExecution true
    Then the build succeeds with parallelExecution true

  # CNV-12.1 — Bump fallbacks (kotlin-test-junit5 2.4.10, BOM 0.0.13)
  Scenario: Fallback versions are up-to-date
    Given a project applies the conventions plugin without version catalog
    Then the testImplementation configuration contains workspace-bom version "0.0.13"
    And the testImplementation configuration contains kotlin-test-junit5 version "2.4.10"

  # CNV-12.2 — fixAnnotationsConflict
  Scenario: Extension has fixAnnotationsConflict default false
    Given a project applies the conventions plugin
    Then the gradlePluginConventions extension has fixAnnotationsConflict default false

  Scenario: fixAnnotationsConflict can be enabled via DSL
    Given a project applies the conventions plugin with fixAnnotationsConflict true
    Then the build succeeds with fixAnnotationsConflict true
