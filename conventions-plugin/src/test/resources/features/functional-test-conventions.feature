Feature: Functional Test Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the functional-test conventions plugin
  So that my project has a GradleTestKit functionalTest source set

  Scenario: Plugin creates functionalTest source set
    Given a project applies the functional-test plugin
    Then the functionalTest source set is created

  Scenario: Plugin registers functionalTest task
    Given a project applies the functional-test plugin
    Then the functionalTest task is registered

  Scenario: Plugin wires check task dependency
    Given a project applies the functional-test plugin
    Then the check task depends on functionalTest

  # CNV-10.3 — extendsFrom(testImplementation)
  Scenario: Plugin extends functionalTest from testImplementation
    Given a project applies the functional-test plugin with java plugin
    Then the functionalTest implementation configuration extends testImplementation

  # CNV-10.3 — additionalDependencies
  Scenario: Plugin supports additional dependencies
    Given a project applies the functional-test plugin with additional dependencies
    Then the functionalTest implementation configuration contains the additional dependency
