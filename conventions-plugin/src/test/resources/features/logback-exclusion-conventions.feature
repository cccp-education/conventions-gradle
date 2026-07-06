Feature: Exclude logback-classic from test classpaths

  As a plugin developer in the cccp-education workspace
  I want the logback exclusion convention applied
  So that my tests do not conflict with the JRuby embedded by asciidoctor

  Scenario: Plugin excludes logback-classic from test configurations
    Given a project applying the logback exclusion convention
    Then the "testRuntimeClasspath" configuration excludes logback-classic without excluding slf4j
    And the "testImplementation" configuration excludes logback-classic without excluding slf4j
    And the "functionalTestRuntimeClasspath" configuration excludes logback-classic without excluding slf4j

  Scenario: Plugin preserves slf4j-api in test configurations
    Given a project applying the logback exclusion convention
    Then the "testImplementation" configuration does not exclude slf4j-api