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
        new File(base, "settings.json").delete()
        new File(base + ".repos/", "repo7.groovy").delete()
    }

    @BeforeClass
    static void setup() {
        clean()

        def repo7 = new File(base + ".repos/", "repo7.groovy")

        repo7 << """
repo {
    name 'repo7'
    path 'ok'
}
"""

        webServer = new WebServer(
            port: port,
            workspace: base,
            appLauncher: "appLauncher"
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
    void api() {
        assertNotNull get("v1")
    }

    @Test
    void run() {
        assertNotNull get("run")
    }

    @Test
    void run_owners() {
        assertNotNull post("run/owners")
    }

    @Test
    void run_names() {
        assertNotNull get("run/names")
    }

    @Test
    void run_search() {
        def response = postJson("run", [name: 'Kubernetes'])
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertEquals 1, json.count
        assertEquals 8, json.total
    }

    @Test
    void run_stage_and_unstage_id() {
        post("run/stage?id=[Kubernetes]%20undefined")

        def responseStage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assertNotNull responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assertNotNull jsonStage
        assertNotNull jsonStage.nodes
        assertTrue jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstage?id=[Kubernetes]%20undefined")

        def responseUnstage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assertNotNull responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assertNotNull jsonUnstage
        assertNotNull jsonUnstage.nodes
        assertTrue jsonUnstage.nodes.isEmpty()
    }

    @Test
    void run_stage_and_unstage_owner() {
        post("run/stage?owner=Kubernetes")

        def responseStage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assertNotNull responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assertNotNull jsonStage
        assertNotNull jsonStage.nodes
        assertTrue jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstage?owner=Kubernetes")

        def responseUnstage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assertNotNull responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assertNotNull jsonUnstage
        assertNotNull jsonUnstage.nodes
        assertTrue jsonUnstage.nodes.isEmpty()
    }

    @Test
    void run_stageAll_and_unstageAll() {
        post("run/stageAll")

        def responseStage = post("run/selected")
        assertNotNull responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assertNotNull jsonStage
        assertNotNull jsonStage.nodes
        assertTrue jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstageAll")

        def responseUnstage = post("run/selected")
        assertNotNull responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assertNotNull jsonUnstage
        assertNotNull jsonUnstage.nodes
        assertTrue jsonUnstage.nodes.isEmpty()
    }

    @Test
    void repos() {
        def response = get("repos")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertNotNull json
        assertEquals 7, json.total
        assertEquals 7, json.descriptors.size()
        assertTrue json.descriptors.any { it.name == "repo1"}
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
    void repos_selected() {

        def responseBeforeStage = postJson ("repos", [selected: true, staged: false])
        assertNotNull responseBeforeStage

        def jsonBeforeStage = new JsonSlurper().parseText(responseBeforeStage)

        assertNotNull jsonBeforeStage
        assertTrue jsonBeforeStage.descriptors.isEmpty()

        post("run/stage?id=[Kubernetes]%20undefined")

        def responseAfterStage = postJson ("repos", [selected: true, staged: false])
        assertNotNull responseAfterStage

        def jsonAfterStage = new JsonSlurper().parseText(responseAfterStage)

        assertNotNull jsonAfterStage
        assertEquals 2, jsonAfterStage.descriptors.size()

        post("run/unstage?id=[Kubernetes]%20undefined")
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

        post("repos/source?name=repo7", sourceText)

        def response = get("repos/view?name=repo7")
        assertNotNull response

        def json = new JsonSlurper().parseText(response)

        assertNotNull json
        assertEquals "repo7", json.name
        assertTrue json.script.text.contains("my-src")
    }

    @Test
    void repo_stage_and_unstage() {

        def stageRepoName = 'repo1'
        post("repos/stage?name=${stageRepoName}")

        def responseStage = post("repos")
        assertNotNull responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assertNotNull jsonStage
        assertNotNull jsonStage.descriptors

        def repoStaged = jsonStage.descriptors.find { it.staged }
        assertNotNull repoStaged
        assertEquals stageRepoName, repoStaged.name

        post("repos/unstage?name=${stageRepoName}")

        def responseUnstage = post("repos")
        assertNotNull responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assertNotNull jsonUnstage
        assertNotNull jsonUnstage.descriptors
        assertFalse jsonUnstage.descriptors.any { it.staged }
    }

    @Test
    void repo_stageAll_and_unstageAll() {
        post("repos/stageAll")

        def responseStage = post("repos")
        assertNotNull responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assertNotNull jsonStage
        assertNotNull jsonStage.descriptors
        assertFalse jsonStage.descriptors.any { !it.staged }

        post("repos/unstageAll")

        def responseUnstage = post("repos")
        assertNotNull responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assertNotNull jsonUnstage
        assertNotNull jsonUnstage.descriptors
        assertFalse jsonUnstage.descriptors.any { it.staged }
    }

    @Test
    void repo_applyDefaultAll_and_resetAll() {

        // Create repos folder ref and delete existing json files
        def parametersFolder = new File(base, ".repos/")
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

    @Test
    @Ignore
    void execution_start() {
        def responseBefore = get("execution")
        assertNotNull responseBefore

        def jsonBefore = new JsonSlurper().parseText(responseBefore)

        assertNotNull jsonBefore
        assertFalse jsonBefore.running

        post("run/stage?id=[Kubernetes]%20undefined")
        def responseStart = post("execution/start")
        assertNotNull responseStart

        def jsonStart = new JsonSlurper().parseText(responseStart)

        assertNotNull jsonStart
        assertNotNull jsonStart.files
        assertTrue jsonStart.files.any { it.contains("repoB") }

        def responseAfter = get("execution")
        assertNotNull responseAfter

        def jsonAfter = new JsonSlurper().parseText(responseAfter)

        assertNotNull jsonAfter
        assertTrue jsonAfter.running

        def jsonEnd

        int count = 50
        while(count > 0) {
            count--

            sleep(500)

            def responseEnd = get("execution")
            assertNotNull responseEnd

            jsonEnd = new JsonSlurper().parseText(responseEnd)
            assertNotNull jsonEnd

            if (!jsonEnd.running)
                break
        }

        // Let execution close
        sleep(500)

        def responseEnd = get("execution")
        assertNotNull responseEnd

        jsonEnd = new JsonSlurper().parseText(responseEnd)
        assertFalse jsonEnd.running

        def responseReview = get("review")
        assertNotNull responseReview

        def jsonReview = new JsonSlurper().parseText(responseReview)

        assertNotNull jsonReview
        assertNotNull jsonReview.baseExecution
        assertNotNull jsonReview.lastExecution
        assertNotNull jsonReview.lines
        assertNotNull jsonReview.stats

        def responsePromote = post("review/promote")
        assertNotNull responsePromote

        def jsonPromote = new JsonSlurper().parseText(responsePromote)
        assertNotNull jsonPromote

        assertEquals "promoted", jsonPromote.result
    }

    @Test
    @Ignore
    void execution_stop() {
        def responseBefore = get("execution")
        assertNotNull responseBefore

        def jsonBefore = new JsonSlurper().parseText(responseBefore)

        assertNotNull jsonBefore
        assertFalse jsonBefore.running

        post("run/stage?id=[Kubernetes]%20undefined")
        post("execution/start")

        def responseMiddle = get("execution")
        assertNotNull responseMiddle

        def jsonMiddle = new JsonSlurper().parseText(responseMiddle)

        assertNotNull jsonMiddle
        assertTrue jsonMiddle.running

        sleep(50)
        post("execution/stop")
        sleep(500)

        def responseEnd = get("execution")
        assertNotNull responseEnd

        def jsonEnd = new JsonSlurper().parseText(responseEnd)

        assertNotNull jsonEnd
        assertFalse jsonEnd.running
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