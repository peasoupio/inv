import groovy.json.JsonSlurper

inv {
    markdown '''
Provide easy-to-use HTTP request methods.
'''

    broadcast $inv.HTTP using {
        markdown '''
Returns a new RequestHandler.  
Methods:
```
    $http.newRequest: Create the default HTTP request (simple)
    $http.newSimpleRequest: Create a simple request.
```
'''
        ready { return new RequestHandler() }
    }
}

class RequestHandler {

    /**
     * Creates a new request.
     * By default it uses a SimpleRequest.
     * @param url String representation of the URL
     * @return A new SimpleHttpRequest instance.
     */
    SimpleHttpRequest newRequest(String url) {
        return this.newSimpleRequest(url)
    }

    /**
     * Creates a new SimpleHttpRequest.
     * By default it uses a SimpleRequest.
     * @param url String representation of the URL
     * @return A new SimpleHttpRequest instance.
     */
    SimpleHttpRequest newSimpleRequest(String url) {
        return new SimpleHttpRequest(url)
    }

}

class SimpleHttpRequest {

    final private HttpURLConnection conn

    private boolean doValidations = true
    private String sendCharset = "UTF-8"
    private Integer connectionTimeout = null
    private Integer readTimeout = null

    private String responseText = null
    private Integer responseCode = null

    final String url

    SimpleHttpRequest(String url) {
        assert url

        this.url = url
        this.conn = (HttpURLConnection) new URL(url).openConnection()
    }

    SimpleHttpRequest timeouts(int connection, int read) {
        assert connection >= 0
        assert read >= 0

        this.connectionTimeout = connection
        this.readTimeout = read

        return this
    }

    SimpleHttpRequest charsets(String sendCharset) {
        assert sendCharset

        this.sendCharset = sendCharset

        return this
    }

    SimpleHttpRequest header(String property, String value) {
        assert property
        assert value

        this.conn.setRequestProperty(property, value)

        return this
    }

    SimpleHttpRequest method(String method) {
        assert method

        this.conn.requestMethod = method

        return this
    }

    SimpleHttpRequest parameter(String property, String value) {
        assert property
        assert sendCharset

        this.conn.setDoOutput(true)

        def bytes = "${property}=${value}".getBytes(this.sendCharset)
        this.conn.outputStream.write(bytes)

        return this
    }

    SimpleHttpRequest send(String data = null) {
        if (data) {
            assert this.sendCharset

            this.conn.setDoOutput(true)
            this.conn.outputStream.write(data.getBytes(this.sendCharset))
        }

        doSend()

        return this
    }

    // When sent

    String toText() { this.responseText }
    Object toJson() { return new JsonSlurper().parseText(this.responseText) }

    int status() { this.responseCode }
    boolean valid() { return this.responseCode >= 200 && this.responseCode < 400 }

    private void doSend() {

        if (this.connectionTimeout)
            this.conn.connectTimeout = connectionTimeout
        else if (this.doValidations)
            println "[WARNING] ${this.url}: connection timeout not defined"

        if (this.readTimeout)
            this.conn.readTimeout = this.readTimeout
        else if (this.doValidations)
            println "[WARNING] ${this.url}: read timeout not defined"

        this.responseText = this.conn.inputStream.text
        this.responseCode = this.conn.responseCode
    }
}

