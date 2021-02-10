Feature: What are the responses of my Composer requests?
  Caller wants to know the responses of my Composer requests

  Scenario Outline: Composer sends responses
    Given an http script "<httpScript>"
    When I start Composer with the working directory "<workDir>"
    Then I should send requests and recieve responses successfully

    Examples:
      | httpScript                                         | workDir           |

      # Repo
      | io.peasoup.inv.composer.repo3.parameters           | /composer/repo3   |
      | io.peasoup.inv.composer.repo2.add_and_remove       | /composer/repo2   |
      | io.peasoup.inv.composer.repo1.search               | /composer/repo1   |

      # Run
      | io.peasoup.inv.composer.run1.repos                 | /composer/run1    |
      | io.peasoup.inv.composer.run1.staging_unstaging     | /composer/run1    |
      | io.peasoup.inv.composer.run1.runfile               | /composer/run1    |

      # Boot
      | io.peasoup.inv.composer.boot1.empty_folder         | /composer/boot1   |
      | io.peasoup.inv.composer.boot2.existing_folder      | /composer/boot2   |

      # Exec
      | io.peasoup.inv.composer.exec1.start_review_promote | /composer/exec1   |
      | io.peasoup.inv.composer.exec2.stop                 | /composer/exec2   |

      # System
      | io.peasoup.inv.composer.system1.system_initfile    | /composer/system1 |

      # Secure
      | io.peasoup.inv.composer.secure1.admin_privileges   | /composer/secure1 |

