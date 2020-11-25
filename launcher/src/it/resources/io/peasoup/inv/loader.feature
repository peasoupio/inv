Feature: What is the state of my loader execution?
  Caller wants to know the state of his loader execution

  Scenario Outline: Loader execution has an execution state
    Given run pattern "<runPattern>"
    When I start the run execution
    Then I should be told the run execution exit code "<exitCode>"

    Examples:
      | runPattern                            | exitCode |
      | inv-invoker-script.groovy             | 0         |
      | inv-invoker-ivy-script.groovy         | 0         |
      | inv-invoker-restricted-script.groovy  | 3         |
      | inv-invoker-restricted2-script.groovy | 0         |
      | inv-invoker-script-with-debug.groovy  | 0         |