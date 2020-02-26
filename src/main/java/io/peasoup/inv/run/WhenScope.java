package io.peasoup.inv.run;

public enum WhenScope {
    All("ALL"),
    Any("ANY");

    String value;

    WhenScope(String value) {
        this.value = value;
    }
}
