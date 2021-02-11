package io.peasoup.inv

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.peasoup.inv.composer.WebServer

import java.nio.charset.Charset

import static junit.framework.TestCase.assertNotNull

class HttpDescriptor {

    private final WebServer webServer
    private final int port

    private String cookie

    HttpDescriptor(WebServer webServer) {
        this.webServer = webServer
        this.port = webServer.webServerConfigs.port
    }

    Map links() {
        (Map)get("/api/v1") {
            assertNotNull it
            assertNotNull it.links

            it.links
        }
    }

    String getSecurityToken() {
        return webServer.security.generatedToken
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
        def urlStr = "http://127.0.0.1:${port}${context}".toString()
        def connection = (HttpURLConnection)new URL(urlStr).openConnection()
        connection.instanceFollowRedirects = false

        if (cookie)
            connection.setRequestProperty("Cookie", cookie)

        String responseText

        try {
            responseText = connection.inputStream.text
        } catch(Exception ex) {
            responseText = connection.errorStream.text
        }

        if (connection.headerFields["Set-Cookie"])
            cookie = connection.headerFields["Set-Cookie"][0]

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
        def urlStr = "http://127.0.0.1:${port}${context}".toString()
        def connection = (HttpURLConnection)new URL(urlStr).openConnection()
        connection.instanceFollowRedirects = false

        if (cookie)
            connection.setRequestProperty("Cookie", cookie)

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

            try {
                return connection.inputStream.text
            } catch(Exception ex) {
                return connection.errorStream.text
            }
        }

        if (callback)
            return callback(responseText)

        return responseText
    }

}
