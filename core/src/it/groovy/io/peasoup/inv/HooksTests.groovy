package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.repo.HookExecutor
import io.peasoup.inv.repo.RepoExecutor
import io.peasoup.inv.repo.RepoInvoker
import io.peasoup.inv.run.Logger

class HooksTests {

    private static String resourceDir

    private static OutputTester outputTester
    private static ResourceTester resourceTester


    static {
        resourceDir = HooksTests.getResource("/io/peasoup/inv/hooks/").path
        Home.setCurrent(new File(resourceDir))

        outputTester = new OutputTester()
        resourceTester = new ResourceTester(resourceDir)

        RepoInvoker.newCache()
    }

    private File repoFile
    private List logs
    private RepoExecutor.RepoHookExecutionReport report

    @Given("repo file {string}")
    void repo_file(String repoFile) {
        this.repoFile =  new File(resourceTester.interpolate(repoFile))
    }

    @When("I indicate the hook name {string}")
    void i_indicate_hook_name(String hookName) {
        logs = Logger.capture(new LinkedList())

        RepoExecutor exec = new RepoExecutor()
        exec.parse(repoFile)

        def descriptor = exec.repos.values().first()

        report = new RepoExecutor.RepoHookExecutionReport(descriptor.name, descriptor)

        switch(hookName) {
            case "init":
                HookExecutor.init(report)
                break
            case "pull":
                HookExecutor.pull(report)
                break
            case "push":
                HookExecutor.push(report)
                break
            case "version":
                HookExecutor.version(report)
                break
        }
    }

    @Then("I should be told the hook exitCode {string} AND hook stdout log file {string}")
    void i_should_be_told_the_main_exitCode_and_main_stdout_log_file(String exitCode, String stdoutLogFile) {
        outputTester.assertOutput(
                exitCode,
                stdoutLogFile,
                report.exitCode,
                logs
        )
    }
}
