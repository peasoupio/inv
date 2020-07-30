package io.peasoup.inv.utils

class Stdout {


    static synchronized void capture(Closure capture, Closure callback) {
        def stdout = System.out

        def out = new ByteArrayOutputStream()
        def printer = new PrintStream(out)

        System.setOut(printer)

        capture.call()
        callback.call(out.toString())

        System.setOut(stdout)
    }
}
