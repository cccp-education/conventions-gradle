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
    Then the project uses Java 24 source compatibility
    And the project uses Java 24 target compatibility
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
