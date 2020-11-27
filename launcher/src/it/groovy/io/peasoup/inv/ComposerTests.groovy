package io.peasoup.inv

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.composer.WebServer
import spark.Spark
import io.peasoup.inv.loader.GroovyLoader

class ComposerTests {
    int port
    DelegatingScript script

    @Given("an http script {string}")
    void an_http_script(String scriptLocation) {
        // set port
        port = new Random().nextInt(500) + 51000

        def clazz = ComposerTests.classLoader.loadClass(scriptLocation)
        script = (DelegatingScript)clazz.getDeclaredConstructor().newInstance()

        script.setDelegate(new HttpDescriptor(port))
    }

    @When("I start Composer with the working directory {string}")
    void i_start_composer_with_the_working_directory(String workDir) {

        // Start server
        String workspace = ComposerTests.getResource("/io/peasoup/inv" + workDir).path
        new WebServer(
                port: port,
                workspace: workspace,
                appLauncher: "my-app-launcher",
                version: "my-version"
        ).routes()
    }
    @Then("I should send requests and recieve responses successfully")
    void i_should_send_requests_and_recieve_responses_successfully() {
        // Run Http script
        script.run()

        // Stop server
        Spark.stop()
        Spark.awaitStop()
    }

}
