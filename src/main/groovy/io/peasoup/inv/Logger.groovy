package io.peasoup.inv

class Logger {


    static def info(Object arg) {
        println "[INV] ${arg}"
    }

    static def warn(Object arg) {
        println "[WARN] ${arg}"
    }


}
