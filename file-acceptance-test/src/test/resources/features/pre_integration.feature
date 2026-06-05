@Startup
Feature: Pre-integration validation for File acceptance tests

  Scenario: Verify File service is reachable before running acceptance tests
    Given I send get request to version info endpoint
    Then service should respond back with version info in response
