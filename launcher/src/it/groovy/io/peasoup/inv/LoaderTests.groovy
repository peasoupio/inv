package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static junit.framework.TestCase.assertEquals

class LoaderTests {

    String pattern
    int exitCode

    static {
        def loaderRes = LoaderTests.getResource("/io/peasoup/inv/loader/").path
        Home.setCurrent(new File(loaderRes))
    }

    @Given("run pattern {string}")
    void run_pattern(String pattern) {
        this.pattern = pattern
    }

    @When("I start the run execution")
    void i_start_the_run_execution() {
        Main.start("run", *pattern.split(" "))
        exitCode = Main.exitCode()
    }

    @Then("I should be told the run execution exit code {string}")
    void i_should_be_told_the_run_execution_exitCode(String exitCode) {
        assertEquals(Integer.parseInt(exitCode), this.exitCode)
    }
}
