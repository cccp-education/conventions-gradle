Feature: Publishing Conventions
  As a plugin developer in the cccp-education workspace
  I want to apply the publishing conventions plugin to my project
  So that my Maven Central POM is standardised with developers, scm, and licenses

  Scenario: Publishing plugin applies POM metadata
    Given a project applies the publishing plugin
    Then the generated POM has developer id "cccp-education"
    And the generated POM has developer name "CCCP Education"
    And the generated POM has license "Apache-2.0"
    And the generated POM has SCM connection starting with "scm:git"

  Scenario: Publishing plugin signs publications conditionally
    Given a project applies the publishing plugin
    When CI is not set and version is not SNAPSHOT
    Then the signing plugin is applied
    And publications are signed

  Scenario: Publishing plugin supports relocation
    Given a project applies the publishing plugin
    When relocation group "com.old" and artifact "old-artifact" are configured
    Then the generated POM has relocation group "com.old"
    And the generated POM has relocation artifact "old-artifact"
