package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.composer.WebServer
import spark.Spark

class ComposerTests {
    int port
    DelegatingScript script
    String workDir

    @Given("an http script {string}")
    void an_http_script(String scriptLocation) {
        // set port
        port = new Random().nextInt(1500) + 31337

        def clazz = ComposerTests.classLoader.loadClass(scriptLocation)
        script = (DelegatingScript)clazz.getDeclaredConstructor().newInstance()
    }

    @When("I start Composer with the working directory {string}")
    void i_start_composer_with_the_working_directory(String workDir) {
        this.workDir = workDir
    }
    @Then("I should send requests and recieve responses successfully")
    void i_should_send_requests_and_recieve_responses_successfully() {

        // Configure environment
        String workspace = ComposerTests.getResource("/io/peasoup/inv" + workDir).path
        Home.setCurrent(new File(workspace))

        File initFile = new File(workspace, "init.yml")

        // Start server
        def webserver = new WebServer(
                port: port,
                appLauncher: "my-app-launcher",
                version: "my-version",
                initFile: initFile.exists() ? initFile.absolutePath : null
        )
        webserver.routes()

        Spark.awaitInitialization()

        // Run Http script
        script.setDelegate(new HttpDescriptor(webserver))

        try {
            script.run()
        } catch(Exception ex) {
            throw ex
        } finally {
            // Stop server
            Spark.stop()
            Spark.awaitStop()
        }
    }

}
