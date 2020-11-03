package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.run.Logger

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
            if (options[i].startsWith("file:/"))
                options[i] = options[i].replace("file:/" ,resourceDir)

            if (options[i].startsWith("url:/"))
                options[i] = options[i].replace("url:/" ,"https://raw.githubusercontent.com/")
        }
    }

    @When("I execute the cli options")
    void i_execute_the_cli_options() {
        logs = Logger.capture(new LinkedList())
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
}