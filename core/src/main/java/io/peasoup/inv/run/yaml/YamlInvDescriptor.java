package io.peasoup.inv.run.yaml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class YamlInvDescriptor {

    private String name;

    private String path;

    private String markdown;

    private Map<String, String> tags;

    private List<YamlStatementDescriptor> workflow;

    public YamlInvDescriptor() {
        // empty ctor
    }
}
