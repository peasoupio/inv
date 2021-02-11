package io.peasoup.inv

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.peasoup.inv.composer.WebServer
import spark.Filter
import spark.Request
import spark.Response
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

        // Setup request and response loggers
        setupLoggers()

        Thread.sleep(2000)

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

    private setupLoggers() {
        int reqCount = 0
        int resCount = 0

        // Register logging hooks
        Spark.before(new Filter() {
            @Override
            void handle(Request request, Response response) throws Exception {
                println "REQUEST #${++reqCount}: ${request.url()}"
                println "\tHEADERS:"
                request.headers().each {
                    println "\t\t${it}:${request.headers(it)}"
                }

                if (request.body()) {
                    println "\tBODY:"
                    println "\t\t${request.body().trim()}"
                }
            }
        })

        Spark.after(new Filter() {
            @Override
            void handle(Request request, Response response) throws Exception {
                println "RESPONSE #${++resCount}: ${request.url()}"
                response.raw().toString().split(System.lineSeparator()).each {
                    println "\t${it}]"
                }

                if (response.body()) {
                    println "\tBODY:"
                    println "\t\t${response.body().trim()}"
                }
            }
        })
    }

}
