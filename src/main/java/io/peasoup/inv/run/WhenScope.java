package io.peasoup.inv.run;

public enum WhenScope {
    ALL("ALL"),
    ANY("ANY");

    String value;

    WhenScope(String value) {
        this.value = value;
    }
}
