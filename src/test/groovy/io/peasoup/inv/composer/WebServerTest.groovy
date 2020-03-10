package io.peasoup.inv.composer

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.TempHome
import io.peasoup.inv.run.Logger
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import spark.Spark

import java.nio.charset.Charset

@RunWith(TempHome.class)
class WebServerTest {

    static String base = "./src/main/example/composer/"
    static Integer port = 5555
    static WebServer webServer

    static void clean() {
        new File(base, "executions/").deleteDir()
        new File(base, "settings.json").delete()
        new File(base, "parameters/").deleteDir()
        new File(base + "scms/", "scm7.groovy").delete()
    }

    @BeforeClass
    static void setup() {
        clean()

        def scm7 = new File(base + "scms/", "scm7.groovy")

        scm7 << """
scm {
    name 'scm7'
    path 'ok'
}
"""

        webServer = new WebServer(
            port: port,
            workspace: base
        )
        webServer.map()

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
        assert get("api")
    }

    @Test
    void run() {
        assert get("run")
    }

    @Test
    void run_owners() {
        assert get("run/owners")
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
    void scms() {
        def response = get("scms")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.total == 7
        assert json.descriptors.size() == 7
        assert json.descriptors.any { it.name == "scm1"}
    }

    @Test
    void scms_search() {
        def response = postJson("scms", [name: "scm1"])
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.total == 7
        assert json.count == 1
        assert json.descriptors.size() == 1
        assert json.descriptors.any { it.name == "scm1"}
    }

    @Test
    void scms_selected() {

        def responseBeforeStage = postJson ("scms", [selected: true, staged: true])
        assert responseBeforeStage

        def jsonBeforeStage = new JsonSlurper().parseText(responseBeforeStage)

        assert jsonBeforeStage
        assert jsonBeforeStage.descriptors.size() == 0

        post("run/stage?id=[Kubernetes]%20undefined")

        def responseAfterStage = postJson ("scms", [selected: true, staged: true])
        assert responseAfterStage

        def jsonAfterStage = new JsonSlurper().parseText(responseAfterStage)

        assert jsonAfterStage
        assert jsonAfterStage.descriptors.size() > 0

        post("run/unstage?id=[Kubernetes]%20undefined")
    }

    @Test
    void scm_view() {
        def response = get("scms/view?name=scm1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.name == "scm1"
    }

    @Test
    void scm_source() {

        def sourceText = """
scm {
    name 'scm7'
    path 'path'
    src 'my-src'
}
""".getBytes(Charset.forName("UTF-8"))

        post("scms/source?name=scm7", sourceText)

        def response = get("scms/view?name=scm7")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json.name == "scm7"
        assert json.script.text.contains("my-src")
    }

    @Test
    void scm_stage_and_unstage() {

        def stageScmName = 'scm1'
        post("scms/stage?name=${stageScmName}")

        def responseStage = post("scms")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.descriptors

        def scmStaged = jsonStage.descriptors.find { it.staged }
        assert scmStaged
        assert scmStaged.name == stageScmName

        post("scms/unstage?name=${stageScmName}")

        def responseUnstage = post("scms")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.descriptors != null
        assert !jsonUnstage.descriptors.any { it.staged }
    }

    @Test
    void scm_stageAll_and_unstageAll() {
        post("scms/stageAll")

        def responseStage = post("scms")
        assert responseStage

        def jsonStage = new JsonSlurper().parseText(responseStage)

        assert jsonStage
        assert jsonStage.descriptors != null
        assert !jsonStage.descriptors.any { !it.staged }

        post("scms/unstageAll")

        def responseUnstage = post("scms")
        assert responseUnstage

        def jsonUnstage = new JsonSlurper().parseText(responseUnstage)

        assert jsonUnstage
        assert jsonUnstage.descriptors != null
        assert !jsonUnstage.descriptors.any { it.staged }
    }

    @Test
    void scm_applyDefaultAll_and_resetAll() {
        def parametersFolder = new File(base, "parameters/")
        post("run/stageAll")

        //Apply
        assert parametersFolder.listFiles().size() == 0

        def responseBeforeApply = get("scms/view?name=scm1")
        assert responseBeforeApply

        def jsonBeforeApply = new JsonSlurper().parseText(responseBeforeApply)

        assert jsonBeforeApply
        assert !jsonBeforeApply.parameters.any { it.value != null}

        post("scms/applyDefaultAll")

        assert parametersFolder.listFiles().size() == 2 // Only 4 descriptors has parameters on 2 files

        def responseAfterApply = get("scms/view?name=scm1")
        assert responseAfterApply

        def jsonAfterApply = new JsonSlurper().parseText(responseAfterApply)

        assert jsonAfterApply
        assert !jsonAfterApply.parameters.any { it.value != it.defaultValue}

        //Reset
        post("scms/resetAll")

        def responseAfteReset = get("scms/view?name=scm1")
        assert responseAfterApply

        def jsonAfterReset = new JsonSlurper().parseText(responseAfteReset)

        assert jsonAfterReset
        assert !jsonAfterReset.parameters.any { it.defaultValue && it.defaultValue == it.value}

        post("run/unstageAll")
    }

    @Test
    void scm_parameters() {
        def parameterValue = new Date().time.toString()

        postJson("scms/parameters?name=scm1&parameter=staticList", [parameterValue: parameterValue])

        def response = get("scms/view?name=scm1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json

        def parameter = json.parameters.find {it.name == "staticList" }

        assert parameter
        assert parameter.value == parameterValue
    }

    @Test
    void scm_parametersValues() {
        def response = get("scms/parametersValues?name=scm1")
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
        assert jsonStart.files.any { it.contains("scmB.groovy") }

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
        sleep(100)

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

        def responseEnd = get("execution")
        assert responseEnd

        def jsonEnd = new JsonSlurper().parseText(responseEnd)

        assert jsonEnd
        assert jsonEnd.running == false
    }
    @Test
    void execution_logs() {
        def messages = ['my-message']
        webServer.exec.messages = [messages]

        def response = get("/execution/logs/0")
        assert response

        def jsonMessages = new JsonSlurper().parseText(response)
        assert jsonMessages instanceof List
        assert jsonMessages[0] == messages[0]
    }

    String get(String context) {
        return new URL("http://127.0.0.1:${port}/${context}".toString()).openConnection().inputStream.text
    }

    String postJson(String context, Map object) {
        return post(context, JsonOutput.toJson(object).getBytes(Charset.forName("UTF-8")))
    }

    String post(String context, byte[] data = new byte[]{}) {
        def connection = new URL("http://0.0.0.0:${port}/${context}".toString()).openConnection() as HttpURLConnection

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