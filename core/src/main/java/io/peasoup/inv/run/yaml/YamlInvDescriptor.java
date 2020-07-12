package io.peasoup.inv.run.yaml;

import java.util.List;
import java.util.Map;

public class YamlInvDescriptor {

    private String name;

    private String path;

    private Map<String, String> tags;

    private List<YamlStatementDescriptor> workflow;

    public YamlInvDescriptor() {
        // empty ctor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public List<YamlStatementDescriptor> getWorkflow() {
        return workflow;
    }

    public void setWorkflow(List<YamlStatementDescriptor> workflow) {
        this.workflow = workflow;
    }
}
