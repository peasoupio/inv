package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class WhenType {

    private final WhenData data;

    public WhenType(WhenData data) {
        if (data == null) {
            throw new IllegalArgumentException("Data is required");
        }

        this.data = data;
    }

    public WhenEvent name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        data.setType(Types.Name);
        data.setValue(name);

        return new WhenEvent(data);
    }

    public WhenEvent tag(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags is required and not empty");
        }

        data.setType(Types.Tags);
        data.setValue(tags);

        return new WhenEvent(data);
    }

    enum Types {
        Name("NAME"),
        Tags("TAGS");

        String value;

        Types(String value) {
            this.value = value;
        }
    }
}
