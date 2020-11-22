package io.peasoup.inv;

public class MissingOptionException extends Exception {
    public MissingOptionException(final String option, String helpLink) {
        super("Option '" + option + "' is not valid. Please visit " + helpLink + " for more information");
    }
}
