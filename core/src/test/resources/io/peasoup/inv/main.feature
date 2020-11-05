Feature: What is the stdout of my cli options ?
  Caller wants to know the stdout for specific client options

  Scenario Outline: Options are valid
    Given cli options "<cliOptions>"
    When I execute the cli options upon the working directory "<workingDir>"
    Then I should be told the exitCode "<exitCode>" AND stdout log file "<stdoutLogFile>"

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
      # GRAPH
      | graph file:/graph/run1.txt                                           |                        | 0        | /io/peasoup/inv/main/graph/stdout10.txt            |
      # Syntax
      | syntax file:/syntax/valid.groovy                                     |                        | 0        | /io/peasoup/inv/main/syntax/stdout15.txt           |
      | syntax file:/syntax/invalid.groovy                                   |                        | 2        | /io/peasoup/inv/main/syntax/stdout16.txt           |
      # Delta
      | delta file:/delta/run1.txt file:/delta/run1.txt                      |                        | 0        | /io/peasoup/inv/main/delta/stdout20.txt            |
      # Repo
      | repo get -rp url:/peasoupio/inv-public-repo/master/net/http/repo.yml | file:/repo/get         | 0        | /io/peasoup/inv/main/repo/stdout40.txt             |
      | repo get -rp url:/peasoupio/inv-public-repo/master/net/http/repo.yml  | file:/repo/get         | 0        | /io/peasoup/inv/main/repo/stdout41.txt             |
      | repo test                                                            | file:/repo/testsuccess | 0        | /io/peasoup/inv/main/repo/testsuccess/stdout30.txt |
      | repo test                                                            | file:/repo/testfailed  | 2        | /io/peasoup/inv/main/repo/testfailed/stdout31.txt  |