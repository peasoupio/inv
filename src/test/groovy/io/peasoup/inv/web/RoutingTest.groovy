package io.peasoup.inv.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import spark.Spark

import java.nio.charset.Charset

class RoutingTest {

    static String base = "./src/main/example/web/"
    static Integer port = 62314

    static void clean() {
        def settingsFile = new File(base, "settings.json")
        if (settingsFile.exists())
            settingsFile.delete()

        def parametersFolder = new File(base, "parameters/")
        if (parametersFolder.exists())
            parametersFolder.deleteDir()

        parametersFolder.mkdir()

        def scm7 = new File(base + "scms/", "scm7.groovy")
        if (scm7.exists())
            scm7.delete()
    }

    @BeforeClass
    static void setup() {

        clean()

        def scm7 = new File(base + "scms/", "scm7.groovy")

        scm7 << """
'scm7' {
}
"""

        new Routing(
            port: port,
            runLocation: base,
            scmsLocation: base + "scms",
            parametersLocation: base + "parameters"
        ).map()
    }

    @AfterClass
    static void close() {

        clean()

        // Spark stop
        Spark.stop()
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
    void run_stage_and_unstage() {
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
        assert json.descriptors.size() == 1
        assert json.descriptors.any { it.name == "scm1"}
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
'scm7' {
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
        assert !jsonAfterApply.parameters.any { it.value == null}

        //Reset
        post("scms/resetAll")

        def responseAfteReset = get("scms/view?name=scm1")
        assert responseAfterApply

        def jsonAfterReset = new JsonSlurper().parseText(responseAfteReset)

        assert jsonAfterReset
        assert !jsonAfterReset.parameters.any { it.value != null}

        post("run/unstageAll")
    }

    @Test
    void scm_parameters() {
        def parameterValue = new Date().time.toString()

        postJson("scms/parameters?name=scm1&parameter=branch", [parameterValue: parameterValue])

        def response = get("scms/view?name=scm1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json

        def parameter = json.parameters.find {it.name == "branch" }

        assert parameter
        assert parameter.value == parameterValue
    }

    @Test
    void scm_parametersValues() {
        def response = get("scms/parametersValues?name=scm1")
        assert response

        def json = new JsonSlurper().parseText(response)

        assert json
        assert json["branch"]
        assert json["branch"].size() == 2
        assert json["branch"].any { it == "1" }
        assert json["param2"]
        assert json["param2"].size() == 2
        assert json["param2"].any { it == "my" }
    }

    String get(String context) {
        return new URL("http://127.0.0.1:${port}/${context}".toString()).openConnection().inputStream.text
    }

    String postJson(String context, Map object) {
        return post(context, JsonOutput.toJson(object).getBytes(Charset.forName("UTF-8")))
    }

    String post(String context, byte[] data = new byte[]{}) {
        def connection = new URL("http://127.0.0.1:${port}/${context}".toString()).openConnection() as HttpURLConnection

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