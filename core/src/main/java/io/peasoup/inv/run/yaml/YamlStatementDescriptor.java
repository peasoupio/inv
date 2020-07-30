package io.peasoup.inv.run.yaml;

public class YamlStatementDescriptor {

    private YamlRequireStatementDescriptor require;

    private YamlBroadcastStatementDescriptor broadcast;

    public YamlStatementDescriptor() {
        // empty ctor
    }

    public YamlRequireStatementDescriptor getRequire() {
        return require;
    }

    public void setRequire(YamlRequireStatementDescriptor require) {
        this.require = require;
    }

    public YamlBroadcastStatementDescriptor getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(YamlBroadcastStatementDescriptor broadcast) {
        this.broadcast = broadcast;
    }
}
