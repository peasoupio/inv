package io.peasoup.inv.cli

interface CliCommand {

    /**
        Do the actual command call
     */
    int call(Map args)

    /**
        Gets whether or not this command may roll a successful or a failing run
     */
    boolean rolling()

    /**
     *  Gets the usage of this command
     */
    String usage();



}