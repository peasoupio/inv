package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.run.Logger
import spark.utils.StringUtils

import static org.junit.Assert.assertEquals

class MainTest {

    private String resourceDir
    private String[] options
    private List logs

    MainTest() {
        this.resourceDir = MainTest.getResource("/io/peasoup/inv/main/").path
        Home.setCurrent(new File(this.resourceDir))
    }

    @Given("cli options {string}")
    void cli_options(String cliOptions) {

        options = cliOptions.split(" ")
        for (i in 0..<options.length) {
            options[i] = interpolate(options[i])
        }
    }

    @When("I execute the cli options upon the working directory {string}")
    void i_execute_the_cli_options(String workingDir) {
        logs = Logger.capture(new LinkedList())

        // Set current home if not empty (otherwise use default)
        if (StringUtils.isNotEmpty(workingDir))
            Home.setCurrent(new File(interpolate(workingDir)))

        Main.start(options)
    }
    @Then("I should be told the exitCode {string} AND stdout log file {string}")
    void i_should_be_told_the_stdout_log_file(String exitCode, String stdoutLogFile) {
        assertEquals Integer.parseInt(exitCode), Main.exitCode

        def expected = new File(MainTest.getResource(stdoutLogFile).path).readLines()
        def actual = new ArrayList<>()

        // Make sure every line seperator are counted as a single line
        for(String log : logs) {
            actual.addAll(log.split(System.lineSeparator()))
        }

        assertEquals String.join(System.lineSeparator(), actual), expected.size(), actual.size()

        for (i in 0..<expected.size()) {
            String actualStr = actual.get(i)
            String expectedStr = expected.get(i)
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")


        }
    }

    private String interpolate(String value) {
        if (value.startsWith("file:/"))
            value = value.replace("file:/" ,resourceDir)

        if (value.startsWith("url:/"))
            value = value.replace("url:/" ,"https://raw.githubusercontent.com/")

        return value
    }


}