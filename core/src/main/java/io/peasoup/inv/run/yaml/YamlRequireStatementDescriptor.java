package io.peasoup.inv.run.yaml;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YamlRequireStatementDescriptor {

    private String name;

    private Object id;

    private String markdown;

    private String resolved;

    private String unresolved;

    private Boolean unbloatable;

    private Boolean defaults;

    public YamlRequireStatementDescriptor() {
        // empty ctor
    }
}
