Feature: Kover threshold check resolves the real report path
  As a plugin developer in the cccp-education workspace
  I want the kover conventions threshold check to read the report kover actually writes
  So that coverage gating works without a per-project workaround

  Scenario: Threshold check succeeds on the kover default report location
    Given a project applies the kover conventions plugin with a covered source
    And the kover threshold is 0
    When the project runs the kover threshold check
    Then the kover threshold check succeeds
    And the kover threshold check reports instruction coverage

  Scenario: Threshold check honours a custom xml report location
    Given a project applies the kover conventions plugin with a covered source
    And the kover threshold is 0
    And the kover xml report is configured at "reports/kover/custom/coverage.xml"
    When the project runs the kover threshold check
    Then the kover threshold check succeeds
    And the custom xml report "reports/kover/custom/coverage.xml" exists

  Scenario: Threshold check fails when coverage is below the threshold
    Given a project applies the kover conventions plugin with a covered source
    And the kover threshold is 101
    When the project runs the kover threshold check
    Then the kover threshold check fails
