Feature: What is the stdout of my main cli options?
  Caller wants to know the stdout for specific main cli options

  Scenario Outline: Main with cli options has stdout
    Given cli options "<cliOptions>"
    When I execute main upon the working directory "<workingDir>"
    Then I should be told the main exitCode "<exitCode>" AND main stdout log file "<stdoutLogFile>"

    Examples:
      | cliOptions                                                           | workingDir             | exitCode | stdoutLogFile                                      |
      # RUN OPTION
      | run main1.groovy                                                     | file:/run              | 0        | /io/peasoup/inv/main/run/stdout1.txt               |
      | run -s main1.groovy                                                  | file:/run              | 0        | /io/peasoup/inv/main/run/stdout2.txt               |
      | run main1.groovy main2.groovy                                        | file:/run              | 0        | /io/peasoup/inv/main/run/stdout3.txt               |
      | run main*.groovy                                                     | file:/run              | 0        | /io/peasoup/inv/main/run/stdout4.txt               |
      | run pattern/*.groovy                                                 | file:/run              | 0        | /io/peasoup/inv/main/run/stdout5.txt               |
      | run main2.groovy                                                     | file:/run              | 3        | /io/peasoup/inv/main/run/stdout6.txt               |
      | run main3.yml                                                        | file:/run              | 0        | /io/peasoup/inv/main/run/stdout7.txt               |
      | run main3.yml main4.yml                                              | file:/run              | 0        | /io/peasoup/inv/main/run/stdout8.txt               |
      | run main*.*                                                          | file:/run              | 0        | /io/peasoup/inv/main/run/stdout9.txt               |
      # GRAPH
      | graph file:/graph/run1.txt                                           |                        | 0        | /io/peasoup/inv/main/graph/stdout1.txt             |
      # Syntax
      | syntax file:/syntax/valid.groovy                                     |                        | 0        | /io/peasoup/inv/main/syntax/stdout1.txt            |
      | syntax file:/syntax/invalid.groovy                                   |                        | 2        | /io/peasoup/inv/main/syntax/stdout2.txt            |
      # Delta
      | delta file:/delta/run1.txt file:/delta/run1.txt                      |                        | 0        | /io/peasoup/inv/main/delta/stdout1.txt             |
      # Repo
      | repo get -rp url:/peasoupio/inv-public-repo/master/net/http/repo.yml | file:/repo/get         | 0        | /io/peasoup/inv/main/repo/stdout1.txt              |
      | repo get -rp url:/peasoupio/inv-public-repo/master/net/http/repo.yml | file:/repo/get         | 0        | /io/peasoup/inv/main/repo/stdout2.txt              |
      | repo test                                                            | file:/repo/testsuccess | 0        | /io/peasoup/inv/main/repo/testsuccess/stdout20.txt |
      | repo test                                                            | file:/repo/testfailed  | 2        | /io/peasoup/inv/main/repo/testfailed/stdout25.txt  |