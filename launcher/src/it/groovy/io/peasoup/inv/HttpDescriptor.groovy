package io.peasoup.inv

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.charset.Charset

class HttpDescriptor {

    private final int port

    HttpDescriptor(int port) {
        this.port = port
    }

    void get(String context, Closure callback) {
        String responseText = new URL("http://127.0.0.1:${port}/${context}".toString()).openConnection().inputStream.text
        Map responseData = new JsonSlurper().parseText(responseText) as Map

        if (callback)
            callback.call(responseData)
    }

    void post(String context, Map data, Closure callback) {

        def bytesData = JsonOutput.toJson(data).getBytes(Charset.forName("UTF-8"))
        def connection = new URL("http://0.0.0.0:${port}/${context}".toString()).openConnection() as HttpURLConnection

        String responseText = connection.with {
            setDoOutput(true)
            setRequestMethod("POST")

            setRequestProperty("Content-type", "text/plain")

            outputStream.write(bytesData)
            outputStream.flush()

            return connection.inputStream.text
        }

        Map responseData = new JsonSlurper().parseText(responseText) as Map

        if (callback)
            callback.call(responseData)
    }

}
