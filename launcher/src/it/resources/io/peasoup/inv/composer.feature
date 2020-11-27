Feature: What are the responses of my Composer requests?
  Caller wants to know the responses of my Composer requests

  Scenario Outline: Composer sends responses
    Given an http script "<httpScript>"
    When I start Composer with the working directory "<workDir>"
    Then I should send requests and recieve responses successfully

    Examples:
      | httpScript                         | workDir         |
      # Boot
      | io.peasoup.inv.composer.boot1.test | /composer/boot1 |
      | io.peasoup.inv.composer.boot2.test | /composer/boot2 |
