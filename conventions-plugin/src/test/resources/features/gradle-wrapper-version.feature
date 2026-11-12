Feature: Gradle wrapper version conventions
  As a workspace maintainer
  I want every build's Gradle wrapper to match a single source of truth
  So that a Gradle upgrade is centralised and enforced by every CI

  # CNV-13.3 — the wrapper version lives in conventions gradle/libs.versions.toml
  # and verifyGradleWrapper (wired to check) fails any drifting build.
  Scenario: A matching wrapper passes the verification
    Given a project applies the conventions plugin with wrapper version "9.9.1"
    When I run the wrapper verification
    Then the wrapper verification succeeds

  Scenario: A drifting wrapper fails the verification
    Given a project applies the conventions plugin with wrapper version "9.7.9"
    When I run the wrapper verification expecting failure
    Then the wrapper verification fails naming "9.7.9"
