inv {
    name "app2-springboot"

    // Gather env variables
    def env = System.getenv().collect { k, v -> "$k=$v" }

    // Compile
    step {
        def myMvnProcess = "mvn.cmd -f ${pwd}/app2/complete/pom.xml clean install"
                .execute(env, new File("./"))

        println "Executing Maven compilation on ${pwd}/app2/complete"
        myMvnProcess.consumeProcessOutput()

        myMvnProcess.waitFor()
    }

    // Execute
    step {
        // Start app
        def mySpringBootProcess = "java -jar ${pwd}/app2/complete/target/gs-spring-boot-0.1.0.jar --server.port=19090"
                .execute(env, new File("./"))

        println "Booting SpringBoot ${pwd}/app2/complete"

        // DO NOT Wait for app to startup
        // It will cause the test to fail

        // curl app to make sure it's available
        def myCurlProcess = "curl localhost:19090"
                .execute(env, new File("./"))

        myCurlProcess.consumeProcessOutput()
        myCurlProcess.waitForOrKill(1000)

        println "App2 curl result: ${myCurlProcess.exitValue()}"

        if (!myCurlProcess.exitValue()) {
            broadcast $inv.App(context: "/", port: 19090)
        }

        // Kill it
        mySpringBootProcess.destroyForcibly()
    }
}