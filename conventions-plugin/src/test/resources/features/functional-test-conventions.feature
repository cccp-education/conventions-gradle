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
