package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.Logger
import io.peasoup.inv.TempHome
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import spark.Spark

import java.nio.charset.Charset

import static org.junit.Assert.*

@RunWith(TempHome.class)
class WebServerTest {

    static String base = "../examples/composer/"
    static Integer port = 5555
    static WebServer webServer

    static void clean() {
        new File(base, "executions/").deleteDir()
        new File(base, "settings.yml").delete()
        new File(base + "repos/", "repo7.groovy").delete()
    }

    @BeforeClass
    static void setup() {
        clean()

        def repo7 = new File(base + "repos/", "repo7.groovy")

        repo7 << """
repo {
    name 'repo7'
    path 'ok'
}
"""

        webServer = new WebServer(
            port: port,
            workspace: base,
            appLauncher: "appLauncher",
            version: "my-version"
        )
        webServer.routes()

        sleep(3000)
    }

    @AfterClass
    static void close() {
        clean()

        // Spark stop
        Spark.stop()
        Spark.awaitStop()

        Logger.setCapture(null)
    }

    @Test
    void repos_search() {
        def response = postJson("repos", [name: "repo1"])
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertNotNull json
        assertEquals 7, json.total
        assertEquals 1, json.count
        assertFalse json.descriptors.isEmpty()
        assertTrue json.descriptors.any { it.name == "repo1"}
    }

    @Test
    void repo_view() {
        def response = get("repos/view?name=repo1")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertNotNull json
        assertEquals "repo1", json.name
    }

    @Test
    void repo_source() {

        def sourceText = """
repo {
    name 'repo7'
    path 'path'
    src 'my-src'
}
""".getBytes(Charset.forName("UTF-8"))

        post("repos/source?name=repo7&mimeType=text/x-groovy", sourceText)

        def response = get("repos/view?name=repo7")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertNotNull json
        assertEquals "repo7", json.name
        assertTrue json.script.text.contains("my-src")
    }

    @Test
    void repo_applyDefaultAll_and_resetAll() {

        // Create repos folder ref and delete existing json files
        def parametersFolder = new File(base, "repos/")
        parametersFolder.listFiles()
            .findAll { it.name.endsWith(".json") }
            .each { it.delete() }

        post("run/stageAll")

        //Apply
        assertFalse parametersFolder.listFiles().any { it.name.endsWith(".json")}

        def responseBeforeApply = get("repos/view?name=repo1")
        assertNotNull responseBeforeApply

        def jsonBeforeApply = new JsonSlurper().parseText(responseBeforeApply)

        assertNotNull jsonBeforeApply
        assertFalse jsonBeforeApply.parameters.any { it.value != null}

        post("repos/applyDefaultAll")

        assertTrue parametersFolder.listFiles().findAll { it.name.endsWith(".json")}.size() == 2 // Only 4 descriptors has parameters on 2 files

        def responseAfterApply = get("repos/view?name=repo1")
        assertNotNull responseAfterApply

        def jsonAfterApply = new JsonSlurper().parseText(responseAfterApply)

        assertNotNull jsonAfterApply
        assertFalse jsonAfterApply.parameters.any { it.value != it.defaultValue}

        //Reset
        post("repos/resetAll")

        def responseAfteReset = get("repos/view?name=repo1")
        assertNotNull responseAfterApply

        def jsonAfterReset = new JsonSlurper().parseText(responseAfteReset)

        assertNotNull jsonAfterReset
        assertFalse jsonAfterReset.parameters.any { it.defaultValue && it.defaultValue == it.value}

        post("run/unstageAll")
    }

    @Test
    void repo_parameters() {
        def parameterValue = new Date().time.toString()

        postJson("repos/parameters?name=repo1&parameter=staticList", [parameterValue: parameterValue])

        def response = get("repos/view?name=repo1")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)
        assertNotNull json

        def parameter = json.parameters.find {it.name == "staticList" }

        assertNotNull parameter
        assertEquals parameterValue, parameter.value
    }

    @Test
    void repo_parametersValues() {
        def response = get("repos/parametersValues?name=repo1")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)
        assertNotNull json

        /*
        TODO Still won't work under Travis
        assertNotNull json["command"]
        assertFalse json["command"].isEmpty()

        assertNotNull json["commandFilter"]
        assertFalse json["commandFilter"].isEmpty()
         */
        assertNotNull json["staticList"]
        assertEquals 2, json["staticList"].size()
        assertTrue json["staticList"].any { it == "my" }
    }

    String get(String context) {
        return new URL("http://127.0.0.1:${port}/api/${context}".toString()).openConnection().inputStream.text
    }

    String postJson(String context, Map object) {
        return post(context, JsonOutput.toJson(object).getBytes(Charset.forName("UTF-8")))
    }

    String post(String context, byte[] data = new byte[]{}) {
        def connection = new URL("http://0.0.0.0:${port}/api/${context}".toString()).openConnection() as HttpURLConnection

        return connection.with {
            setDoOutput(true)
            setRequestMethod("POST")

            setRequestProperty("Content-type", "text/plain")

            outputStream.write(data)
            outputStream.flush()

            return connection.inputStream.text
        }
    }

}