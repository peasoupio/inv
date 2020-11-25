Feature: What is the state of my repo execution?
  Caller wants to know the state of his repo execution

  Scenario Outline: Repo execution has an execution state
    Given repo file location "<repoFile>"
    When I start the repo execution
    Then I should be told the repo execution exit code "<exitCode>"

    Examples:
      | repoFile             | exitCode |
      | repo.groovy          | 0        |
      | repo-debug.groovy    | 0        |
      | repo-invalid.groovy  | 4        |
      | repo-multiple.groovy | 0        |
      | repo-relative.groovy | 0        |
