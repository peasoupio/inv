package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

import static junit.framework.TestCase.assertEquals

class RepoTests {

    String repoFile
    int exitCode

    static {
        def loaderRes = RepoTests.getResource("/io/peasoup/inv/repo/").path
        Home.setCurrent(new File(loaderRes))
    }

    @Given("repo file location {string}")
    void repo_file_location(String repoFile) {
        this.repoFile = repoFile
    }

    @When("I start the repo execution")
    void i_start_the_repo_execution() {
        Main.start("repo-run", new File(Home.getCurrent(), repoFile).absolutePath)
        exitCode = Main.exitCode()
    }

    @Then("I should be told the repo execution exit code {string}")
    void i_should_be_told_the_repo_execution_exitCode(String exitCode) {
        assertEquals(Integer.parseInt(exitCode), this.exitCode)
    }
}
