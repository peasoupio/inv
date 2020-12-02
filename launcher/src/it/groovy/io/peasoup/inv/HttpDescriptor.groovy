package io.peasoup.inv

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.charset.Charset

import static junit.framework.TestCase.assertNotNull
import static junit.framework.TestCase.assertNotNull

class HttpDescriptor {

    private final int port

    HttpDescriptor(int port) {
        this.port = port
    }

    Map links() {
        (Map)get("/api/v1") {
            assertNotNull it
            assertNotNull it.links

            it.links
        }
    }

    void waitFor(int waitTill, Closure body) {
        int i = waitTill
        while(i > 0) {
            sleep(1000)
            if (body())
                i = 0
            i--
        }
    }

    Object get(String context) {
        return get(context, null)
    }

    Object get(String context, Closure callback) {
        String responseText = getAsString(context)

        Map responseData = new JsonSlurper().parseText(responseText) as Map

        if (callback)
            return callback.call(responseData)

        return true
    }

    String getAsString(String context, Closure callback = null) {
        String urlStr = "http://127.0.0.1:${port}${context}".toString()
        String responseText = new URL(urlStr).openConnection().inputStream.text
        if (callback)
            return callback(responseText)

        return responseText
    }

    Object post(String context) {
        return post(context, null, null)
    }

    Object post(String context, Closure callback) {
        return post(context, null, callback)
    }

    Object post(String context, Object data, Closure callback) {
        def responseText = postAsString(context, data)
        if (responseText) {
            Map responseData = new JsonSlurper().parseText(responseText) as Map

            if (callback)
                return callback.call(responseData)
        } else {
            if (callback)
                return callback.call()
        }

        return true
    }

    String postAsString(String context, Object data = null, Closure callback = null) {
        String urlStr = "http://127.0.0.1:${port}${context}".toString()
        HttpURLConnection connection = new URL(urlStr).openConnection() as HttpURLConnection

        String responseText = connection.with {
            setDoOutput(true)
            setRequestMethod("POST")

            setRequestProperty("Content-type", "text/plain")

            // to json bytes
            if (data instanceof Map) {
                byte[] bytesData = JsonOutput.toJson(data).getBytes(Charset.forName("UTF-8"))
                outputStream.write(bytesData)
                outputStream.flush()
            }

            // to string bytes
            if (data instanceof String) {
                byte[] bytesData = ((String)data).getBytes("UTF-8")
                outputStream.write(bytesData)
                outputStream.flush()
            }

            return connection.inputStream.text
        }

        if (callback)
            return callback(responseText)

        return responseText
    }

}
