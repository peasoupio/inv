Feature: What is the stdout of repo hooks?
  Caller wants to know the stdout for specific hooks scripts

  Scenario Outline: Hooks with scripts has stdout
    Given repo file "<hookRepofile>"
    When I indicate the hook name "<hookName>"
    Then I should be told the hook exitCode "<exitCode>" AND hook stdout log file "<stdoutLogFile>"

    Examples:
      | hookRepofile               | hookName | exitCode | stdoutLogFile                             |
      | file:/init/init1.yml       | init     | 0        | /io/peasoup/inv/hooks/init/stdout1.txt    |
      | file:/init/init2.yml       | init     | 3        | /io/peasoup/inv/hooks/init/stdout2.txt    |
      | file:/init/init3.yml       | init     | 0        | /io/peasoup/inv/hooks/init/stdout3.txt    |
      | file:/pull/pull1.yml       | pull     | 0        | /io/peasoup/inv/hooks/pull/stdout1.txt    |
      | file:/push/push1.yml       | push     | 0        | /io/peasoup/inv/hooks/push/stdout1.txt    |
      | file:/version/version1.yml | version  | 0        | /io/peasoup/inv/hooks/version/stdout1.txt |
      | file:/version/version2.yml | version  | 0        | /io/peasoup/inv/hooks/version/stdout2.txt |