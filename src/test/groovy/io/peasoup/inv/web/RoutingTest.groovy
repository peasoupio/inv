package io.peasoup.inv.web


import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static spark.Spark.stop

class RoutingTest {

    static Integer port = 62314

    @BeforeClass
    static void setup() {
        String base = "./src/main/example/web"

        new Routing(
            port: port,
            runLocation: base,
            scmsLocation: base + "/scms",
            parametersLocation: base + "/parameters"
        ).map()
    }

    @AfterClass
    static void close() {
        // Spark stop
        stop()
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
    void run_requireBy() {
        println get("run/requiredBy?id=Kubernetes")
    }

    String get(String context) {
        return new URL("http://127.0.0.1:${port}/${context}".toString()).openConnection().inputStream.text
    }

}