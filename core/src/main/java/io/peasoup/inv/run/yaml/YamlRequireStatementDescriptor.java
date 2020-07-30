package io.peasoup.inv.run.yaml;

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

    public String getResolved() {
        return resolved;
    }

    public void setResolved(String resolved) {
        this.resolved = resolved;
    }

    public String getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(String unresolved) {
        this.unresolved = unresolved;
    }

    public Boolean getUnbloatable() {
        return unbloatable;
    }

    public void setUnbloatable(Boolean unbloatable) {
        this.unbloatable = unbloatable;
    }

    public Boolean getDefaults() {
        return defaults;
    }

    public void setDefaults(Boolean defaults) {
        this.defaults = defaults;
    }
}
