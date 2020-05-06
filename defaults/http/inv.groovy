import groovy.json.JsonSlurper

inv {

    broadcast $inv.HTTP using {
        ready {[
                newRequest: { String url ->

                    def r = new SimpleHttpRequest(url)

                    def instance = [:]
                    instance += [
                            timeouts: { int connection, int read ->
                                r.connectionTimeout = connection
                                r.readTimeout = read
                            },
                            charsets: { String send ->
                                assert send

                                r.sendCharset = send
                            },
                            header: { String property, String value ->
                                assert property
                                assert value

                                r.request.setRequestProperty(property, value)

                                return instance
                            },
                            method: { String method ->
                                assert method

                                r.request.requestMethod = method

                                return instance
                            },
                            parameter: { String property, String value ->
                                assert property
                                assert r.sendCharset

                                r.request.setDoOutput(true)

                                def bytes = "${property}=${value}".getBytes(r.sendCharset)
                                r.request.outputStream.write(bytes)

                                return instance
                            },


                            send: { String method = 'GET', String data = null ->

                                assert method

                                if (data) {
                                    assert r.sendCharset

                                    r.request.setDoOutput(true)
                                    r.request.outputStream.write(data.getBytes(r.sendCharset))
                                }

                                r.send()

                                return instance
                            },

                            // When sent

                            toText: { r.responseText },
                            toJson: { return new JsonSlurper().parseText(r.responseText) },

                            status: { r.responseCode },
                            valid: { return r.responseCode >= 200 && r.responseCode < 400 }
                    ]
                }
        ]}
    }

}

class SimpleHttpRequest {

    final HttpURLConnection request
    final String url

    boolean doValidations = true
    String sendCharset = "UTF-8"
    Integer connectionTimeout = null
    Integer readTimeout = null

    String responseText = null
    Integer responseCode = null

    SimpleHttpRequest(String url) {
        assert url

        this.url = url
        this.request = (HttpURLConnection) new URL(url).openConnection()
    }

    void send() {

        if (connectionTimeout)
            request.connectTimeout = connectionTimeout
        else if (doValidations)
            println "[WARNING] ${url}: connection timeout not defined"

        if (readTimeout)
            request.readTimeout = readTimeout
        else if (doValidations)
            println "[WARNING] ${url}: read timeout not defined"

        responseText = request.inputStream.text
        responseCode = request.responseCode
    }
}

