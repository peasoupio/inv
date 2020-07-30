package io.peasoup.inv.cli

interface CliCommand {

    /*
        Do the actual command call
     */
    int call()

    /*
        Determines whether or not this command may roll a successful or a failing run
     */
    boolean rolling()
}