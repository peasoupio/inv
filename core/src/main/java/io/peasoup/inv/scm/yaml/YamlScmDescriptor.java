package io.peasoup.inv.scm.yaml;

import java.util.List;

public class YamlScmDescriptor {

    private String name;

    private String path;

    private String src;

    private String entry;

    private Integer timeout;

    private List<YamlAskDescriptor> ask;

    private YamlHooksDescriptor hooks;

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

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public List<YamlAskDescriptor> getAsk() {
        return ask;
    }

    public void setAsk(List<YamlAskDescriptor> ask) {
        this.ask = ask;
    }

    public YamlHooksDescriptor getHooks() {
        return hooks;
    }

    public void setHooks(YamlHooksDescriptor hooks) {
        this.hooks = hooks;
    }
}
