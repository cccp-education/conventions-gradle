Feature: Lint Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the lint conventions plugin
  So that my project has ktlint configured

  Scenario: Plugin applies ktlint
    Given a project applies the lint plugin
    Then the ktlint plugin is applied

  Scenario: Plugin registers ktlint tasks
    Given a project applies the lint plugin
    Then the ktlint tasks are available
