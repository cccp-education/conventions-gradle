Feature: Cucumber BDD Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the cucumber conventions plugin
  So that my project has Cucumber BDD test infrastructure configured

  Scenario: Plugin creates features and scenarios source dirs
    Given a project applies the cucumber plugin
    Then the features resource directory is configured
    And the scenarios source directory is configured

  Scenario: Plugin registers cucumberTest task
    Given a project applies the cucumber plugin
    Then the cucumberTest task is registered
    And cucumberTest uses JUnit Platform with jupiter excluded

  Scenario: Plugin excludes scenarios from test task
    Given a project applies the cucumber plugin
    Then the test task excludes *.scenarios.* patterns

  Scenario: Plugin wires check task dependency
    Given a project applies the cucumber plugin
    And a smoke feature file exists
    Then the check task runs cucumberTest

  Scenario: Plugin supports additional tasks with runnerClass
    Given a project applies the cucumber plugin with additional tasks and runnerClass
    Then the additional cucumberTestEpic1 task is registered
    And the additional task uses runnerClass filter

  # CNV-7.1 — Plugin ajoute les dépendances cucumber à testImplementation
  Scenario: Plugin adds cucumber dependencies to testImplementation
    Given a project applies the cucumber plugin with dependency inspection
    Then the testImplementation configuration contains cucumber-java
    And the testImplementation configuration contains cucumber-junit-platform-engine
    And the testImplementation configuration contains junit-platform-suite
    And the testImplementation configuration has the workspace-bom platform
