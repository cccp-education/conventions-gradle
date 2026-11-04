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

  # CNV-10.5 — detekt branch coverage (skip on Java 25, apply otherwise)
  Scenario: Plugin handles detekt conditionally based on Java version
    Given a project applies the lint plugin
    Then the plugin does not fail when detekt is conditionally applied

  # CNV-15.2 — the JVM compatibility policy is explicit and future-proof
  Scenario Outline: Detekt compatibility follows the Java major version
    Given the detekt Java version "<version>"
    Then detekt compatibility is "<supported>"

    Examples:
      | version | supported |
      | 21.0.1  | true      |
      | 22      | true      |
      | 25.0.5  | false     |
      | 26      | false     |
      | unknown | false     |
