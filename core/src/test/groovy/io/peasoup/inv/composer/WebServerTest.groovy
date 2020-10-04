package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.Logger
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import spark.Spark

import java.nio.charset.Charset

import static org.junit.Assert.assertFalse

@RunWith(TempHome.class)
class WebServerTest {

    static String base = "../examples/composer/"
    static Integer port = 5555
    static WebServer webServer

    static void clean() {
        new File(base, "executions/").deleteDir()
        new File(base, "settings.json").delete()
        new File(base, "parameters/").deleteDir()
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
            workspace: base
        )
        webServer.routes()

        sleep(3000)
    }

    @AfterClass
    static void close() {

        clean()

        // Spark stop
        Spark.stop()

        Logger.resetCapture()
    }

    @Test
    void api() {
        assert get("v1")
    }

    @Test
    void run() {
        assert get("run")
    }

    @Test
    void run_owners() {
        assert post("run/owners")
    }

    @Test
    void run_names() {
        assert get("run/names")
    }

    @Test
    void run_search() {
        def response = postJson("run", [name: 'Kubernetes'])

        assert response

        def json = new JsonSlurper().parseText(response)

        assert json.count == 1
        assert json.total == 8
    }

    @Test
    void run_stage_and_unstage_id() {
        post("run/stage?id=[Kubernetes]%20undefined")

        def responseStage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.nodes
        assert jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstage?id=[Kubernetes]%20undefined")

        def responseUnstage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.nodes != null
        assert jsonUnstage.nodes.isEmpty()
    }

    @Test
    void run_stage_and_unstage_owner() {
        post("run/stage?owner=Kubernetes")

        def responseStage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.nodes
        assert jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstage?owner=Kubernetes")

        def responseUnstage = get("run/requiredBy?id=[Server]%20[name:server-a]")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.nodes != null
        assert jsonUnstage.nodes.isEmpty()
    }

    @Test
    void run_stageAll_and_unstageAll() {
        post("run/stageAll")

        def responseStage = post("run/selected")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.nodes
        assert jsonStage.nodes.any { it.name == "Kubernetes"}

        post("run/unstageAll")

        def responseUnstage = post("run/selected")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.nodes != null
        assert jsonUnstage.nodes.isEmpty()
    }

    @Test
    void repos() {
        def response = get("repos")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.total == 7
        assert json.descriptors.size() == 7
        assert json.descriptors.any { it.name == "repo1"}
    }

    @Test
    void repos_search() {
        def response = postJson("repos", [name: "repo1"])
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.total == 7
        assert json.count == 1
        assert json.descriptors.size() == 1
        assert json.descriptors.any { it.name == "repo1"}
    }

    @Test
    void repos_selected() {

        def responseBeforeStage = postJson ("repos", [selected: true, staged: false])
        assert responseBeforeStage

        def jsonBeforeStage = new JsonSlurper().parseText(responseBeforeStage)

        assert jsonBeforeStage
        assert jsonBeforeStage.descriptors.size() == 0

        post("run/stage?id=[Kubernetes]%20undefined")

        def responseAfterStage = postJson ("repos", [selected: true, staged: false])
        assert responseAfterStage

        def jsonAfterStage = new JsonSlurper().parseText(responseAfterStage)

        assert jsonAfterStage
        assert jsonAfterStage.descriptors.size() == 2

        post("run/unstage?id=[Kubernetes]%20undefined")
    }

    @Test
    void repo_view() {
        def response = get("repos/view?name=repo1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.name == "repo1"
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
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.name == "repo7"
        assert json.script.text.contains("my-src")
    }

    @Test
    void repo_stage_and_unstage() {

        def stageRepoName = 'repo1'
        post("repos/stage?name=${stageRepoName}")

        def responseStage = post("repos")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.descriptors

        def repoStaged = jsonStage.descriptors.find { it.staged }
        assert repoStaged
        assert repoStaged.name == stageRepoName

        post("repos/unstage?name=${stageRepoName}")

        def responseUnstage = post("repos")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.descriptors != null
        assert !jsonUnstage.descriptors.any { it.staged }
    }

    @Test
    void repo_stageAll_and_unstageAll() {
        post("repos/stageAll")

        def responseStage = post("repos")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.descriptors != null
        assert !jsonStage.descriptors.any { !it.staged }

        post("repos/unstageAll")

        def responseUnstage = post("repos")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.descriptors != null
        assert !jsonUnstage.descriptors.any { it.staged }
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
        assert responseBeforeApply

        def jsonBeforeApply = new JsonSlurper().parseText(responseBeforeApply)

        assert jsonBeforeApply
        assert !jsonBeforeApply.parameters.any { it.value != null}

        post("repos/applyDefaultAll")

        assert parametersFolder.listFiles().findAll { it.name.endsWith(".json")}.size() == 2 // Only 4 descriptors has parameters on 2 files

        def responseAfterApply = get("repos/view?name=repo1")
        assert responseAfterApply

        def jsonAfterApply = new JsonSlurper().parseText(responseAfterApply)

        assert jsonAfterApply
        assert !jsonAfterApply.parameters.any { it.value != it.defaultValue}

        //Reset
        post("repos/resetAll")

        def responseAfteReset = get("repos/view?name=repo1")
        assert responseAfterApply

        def jsonAfterReset = new JsonSlurper().parseText(responseAfteReset)

        assert jsonAfterReset
        assert !jsonAfterReset.parameters.any { it.defaultValue && it.defaultValue == it.value}

        post("run/unstageAll")
    }

    @Test
    void repo_parameters() {
        def parameterValue = new Date().time.toString()

        postJson("repos/parameters?name=repo1&parameter=staticList", [parameterValue: parameterValue])

        def response = get("repos/view?name=repo1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json

        def parameter = json.parameters.find {it.name == "staticList" }

        assert parameter
        assert parameter.value == parameterValue
    }

    @Test
    void repo_parametersValues() {
        def response = get("repos/parametersValues?name=repo1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json

        /*
        TODO Still won't work under Travis
        assert json["command"]
        assert json["command"].size() > 0

        assert json["commandFilter"]
        assert json["commandFilter"].size() > 0
         */
        assert json["staticList"]
        assert json["staticList"].size() == 2
        assert json["staticList"].any { it == "my" }
    }

    @Test
    void execution_start() {
        def responseBefore = get("execution")
        assert responseBefore

        def jsonBefore = new JsonSlurper().parseText(responseBefore)

        assert jsonBefore
        assert jsonBefore.running == false

        post("run/stage?id=[Kubernetes]%20undefined")
        def responseStart = post("execution/start")
        assert responseStart

        def jsonStart = new JsonSlurper().parseText(responseStart)

        assert jsonStart
        assert jsonStart.files
        assert jsonStart.files.any { it.contains("repoB") }

        def responseAfter = get("execution")
        assert responseAfter

        def jsonAfter = new JsonSlurper().parseText(responseAfter)

        assert jsonAfter
        assert jsonAfter.running == true

        def jsonEnd

        int count = 50
        while(count > 0) {
            count--

            sleep(500)

            def responseEnd = get("execution")
            assert responseEnd

            jsonEnd = new JsonSlurper().parseText(responseEnd)

            assert jsonEnd

            if (!jsonEnd.running)
                break
        }

        // Let execution close
        sleep(500)

        def responseEnd = get("execution")
        assert responseEnd

        jsonEnd = new JsonSlurper().parseText(responseEnd)

        assert jsonEnd.running == false

        def responseReview = get("review")
        assert responseReview

        def jsonReview = new JsonSlurper().parseText(responseReview)
        assert jsonReview
        assert jsonReview.baseExecution != null
        assert jsonReview.lastExecution != null
        assert jsonReview.lines
        assert jsonReview.stats

        def responsePromote = post("review/promote")
        assert responsePromote

        def jsonPromote = new JsonSlurper().parseText(responsePromote)
        assert jsonPromote

        assert jsonPromote.result == 'promoted'
    }

    @Test
    @Ignore
    void execution_stop() {
        def responseBefore = get("execution")
        assert responseBefore

        def jsonBefore = new JsonSlurper().parseText(responseBefore)

        assert jsonBefore
        assert jsonBefore.running == false

        post("run/stage?id=[Kubernetes]%20undefined")
        post("execution/start")

        def responseMiddle = get("execution")
        assert responseMiddle

        def jsonMiddle = new JsonSlurper().parseText(responseMiddle)

        assert jsonMiddle
        assert jsonMiddle.running == true

        sleep(50)
        post("execution/stop")
        sleep(500)

        def responseEnd = get("execution")
        assert responseEnd

        def jsonEnd = new JsonSlurper().parseText(responseEnd)

        assert jsonEnd
        assert jsonEnd.running == false
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