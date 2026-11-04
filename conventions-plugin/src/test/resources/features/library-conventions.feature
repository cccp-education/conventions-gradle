Feature: Library Conventions
  As an N0 contract library maintainer in the cccp-education workspace
  I want to apply the library conventions plugin
  So that my contract build is standardised without java-gradle-plugin

  Scenario: Plugin applies the JVM library plugins
    Given a library project applies the library plugin
    Then the library project has the java-library plugin applied
    And the library project has the kotlin-jvm plugin applied

  Scenario: Plugin configures Java 25 and the publication jars
    Given a library project applies the library plugin
    Then the library project uses Java 25 source compatibility
    And the library project uses Java 25 target compatibility
    And the library project has sources jar task
    And the library project has javadoc jar task

  Scenario: Plugin injects the workspace bom and JUnit test dependencies
    Given a library project applies the library plugin with dependency inspection
    Then the library project test dependencies contain the workspace-bom
    And the library project test dependencies contain junit-jupiter

  Scenario: Plugin registers the workspace guards without java-gradle-plugin
    Given a library project applies the library plugin
    Then the library project registers the wrapper guard
    And the library project registers the adoption audit
    And the library project does not apply the gradle plugin convention

  Scenario: Library test options can be overridden via DSL
    Given a library project applies the library plugin with maxHeapSize "2g"
    Then the library project test tasks use maxHeapSize "2g"
