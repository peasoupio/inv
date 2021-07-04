package io.peasoup.inv.run.yaml;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YamlBroadcastStatementDescriptor {

    private String name;

    private Object id;

    private String markdown;

    private String global;

    private String dynamic;

    public YamlBroadcastStatementDescriptor() {
        // empty ctor
    }
}
