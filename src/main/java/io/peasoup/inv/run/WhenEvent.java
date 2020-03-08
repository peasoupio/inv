package io.peasoup.inv.run;

import groovy.lang.Closure;

public class WhenEvent {

    private final WhenData data;

    protected WhenEvent(WhenData data) {
        if (data == null) {
            throw new IllegalArgumentException("Data is required");
        }

        this.data = data;
    }

    void created(Closure body) {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        data.setEvent(Events.CREATED);
        data.setCallback(body);
    }

    void completed(Closure body) {
        if (body == null) {
            throw new IllegalArgumentException("Body is required");
        }

        data.setEvent(Events.COMPLETED);
        data.setCallback(body);
    }

    enum Events {
        CREATED("CREATED"),
        COMPLETED("COMPLETED");

        String value;

        Events(String value) {
            this.value = value;
        }
    }

}
