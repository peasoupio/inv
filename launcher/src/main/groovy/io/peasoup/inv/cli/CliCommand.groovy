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

    /**
     * If set to true, it prevents libraries cross-contamination with INV own libraries, per example used for Composer.
     */
    boolean requireSafeExecutionLibraries();



}