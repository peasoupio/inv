inv {
    name "app1-springboot"

    // Gather env variables
    def env = System.getenv().collect { k, v -> "$k=$v" }

    // Compile
    step {
        def myMvnProcess = "mvn.cmd -f ${pwd}/app1/complete/pom.xml clean install"
                .execute(env, new File("./"))

        println "Executing Maven compilation on ${pwd}/app1/complete"
        myMvnProcess.consumeProcessOutput()

        myMvnProcess.waitFor()
    }

    // Execute
    step {
        // Start app
        def mySpringBootProcess = "java -jar ${pwd}/app1/complete/target/gs-spring-boot-0.1.0.jar --server.port=9090"
                .execute(env, new File("./"))

        println "Booting SpringBoot ${pwd}/app1/complete"

        // curl app to make sure it's available
        def myCurlProcess = "curl localhost:9090"
                .execute(env, new File("./"))

        myCurlProcess.consumeProcessOutput()
        myCurlProcess.waitForOrKill(30000)

        println "App1 curl result: ${myCurlProcess.exitValue()}"

        if (!myCurlProcess.exitValue()) {
            broadcast $inv.App(context: "/", port: 9090)
        }

        // Kill it
        mySpringBootProcess.destroyForcibly()
    }
}