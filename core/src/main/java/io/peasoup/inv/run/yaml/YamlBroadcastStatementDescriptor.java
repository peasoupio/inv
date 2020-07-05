package io.peasoup.inv.run.yaml;

public class YamlBroadcastStatementDescriptor {

    private String name;

    private Object id;

    private String markdown;

    private String ready;

    public YamlBroadcastStatementDescriptor() {
        // empty ctor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    public String getReady() {
        return ready;
    }

    public void setReady(String ready) {
        this.ready = ready;
    }



}
