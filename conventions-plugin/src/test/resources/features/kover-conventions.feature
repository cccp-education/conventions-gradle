Feature: Kover Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the kover conventions plugin to my project
  So that code coverage is configured with Kover

  Scenario: Kover plugin is disabled by default
    Given a project applies the kover conventions plugin
    Then the kover plugin is not applied

  Scenario: Kover plugin is applied when enabled
    Given a project applies the kover conventions plugin with enabled true
    Then the kover plugin is applied

  Scenario: Kover measures the production and functional test source sets only
    Given a project applies the kover conventions plugin with enabled true
    When the project runs the kover sources probe
    Then the measured source sets are exactly "functionalTest" and "main"

  Scenario: Kover attaches the HTML and XML reports to check
    Given a project applies the kover conventions plugin with enabled true
    When the project runs the kover sources probe
    Then the HTML and XML reports are attached to check

