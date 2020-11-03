Feature: What is the stdout of my cli options ?
  Caller wants to know the stdout for specific client options

  Scenario Outline: Options are valid
    Given cli options "<cliOptions>"
    When I execute the cli options
    Then I should be told the exitCode "<exitCode>" AND stdout log file "<stdoutLogFile>"

    Examples:
      | cliOptions                                        | exitCode | stdoutLogFile                            |
      # RUN OPTION
      | run file:/run/main1.groovy                        | 0        | /io/peasoup/inv/main/run/stdout1.txt     |
      | run -s file:/run/main1.groovy                     | 0        | /io/peasoup/inv/main/run/stdout2.txt     |
      | run file:/run/main1.groovy file:/run/main2.groovy | 0        | /io/peasoup/inv/main/run/stdout3.txt     |
      | run /run/main*.groovy                             | 0        | /io/peasoup/inv/main/run/stdout4.txt     |
      | run /run/pattern/*.groovy                         | 0        | /io/peasoup/inv/main/run/stdout5.txt     |
      | run file:/run/main2.groovy                        | 3        | /io/peasoup/inv/main/run/stdout6.txt     |
      # GRAPH
      | graph dot file:/graph/run1.txt                    | 0        | /io/peasoup/inv/main/graph/stdout10.txt  |
      # Syntax
      | syntax file:/syntax/valid.groovy                  | 0        | /io/peasoup/inv/main/syntax/stdout15.txt |
      | syntax file:/syntax/invalid.groovy                | 2        | /io/peasoup/inv/main/syntax/stdout16.txt |
      # Delta
      | delta file:/delta/run1.txt file:/delta/run1.txt   | 0        | /io/peasoup/inv/main/delta/stdout20.txt  |
      # Test
      | test file:/test/success.groovy                    | 0        | /io/peasoup/inv/main/test/stdout30.txt   |
      | test file:/test/failed.groovy                     | 2        | /io/peasoup/inv/main/test/stdout31.txt   |