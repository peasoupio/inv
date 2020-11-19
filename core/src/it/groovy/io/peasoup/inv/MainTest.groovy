package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.Logger
import spark.utils.StringUtils

class MainTest {

    private static String resourceDir
    private static String[] options

    private static OutputTester outputTester
    private static ResourceTester resourceTester

    static {
        resourceDir = MainTest.getResource("/io/peasoup/inv/main/").path
        Home.setCurrent(new File(resourceDir))

        // Clear remaining resources files
        new File(resourceDir, "repo/get").deleteDir()
        new File(resourceDir, "repo/create").deleteDir()
        new File(resourceDir, "init/init1").deleteDir()

        outputTester = new OutputTester()
        resourceTester = new ResourceTester(resourceDir)
    }

    private List logs

    @Given("cli options {string}")
    void cli_options(String cliOptions) {

        options = cliOptions.split(" ")
        for (i in 0..<options.length) {
            options[i] = resourceTester.interpolate(options[i])
        }
    }

    @When("I execute main upon the working directory {string}")
    void i_execute_main_upon_the_working_directory(String workingDir) {
        logs = Logger.capture(new LinkedList())

        // Set current home if not empty (otherwise use default)
        if (StringUtils.isNotEmpty(workingDir))
            Home.setCurrent(new File(resourceTester.interpolate(workingDir)))

        Main.start(options)
    }

    @Then("I should be told the main exitCode {string} AND main stdout log file {string}")
    void i_should_be_told_the_main_exitCode_and_main_stdout_log_file(String exitCode, String stdoutLogFile) {
        outputTester.assertOutput(
                exitCode,
                stdoutLogFile,
                Main.exitCode,
                logs
        )
    }
}