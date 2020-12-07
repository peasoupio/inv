package io.peasoup.inv.run.yaml;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YamlStatementDescriptor {

    private YamlRequireStatementDescriptor require;

    private YamlBroadcastStatementDescriptor broadcast;

    public YamlStatementDescriptor() {
        // empty ctor
    }
}
