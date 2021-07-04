package io.peasoup.inv.run;

import groovy.lang.MetaProperty;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.NotImplementedException;

@AllArgsConstructor
public class BroadcastNullResponseNonMetaclass {

    private final BroadcastResponse broadcastResponse;

    public Object getResponse() {
        return this;
    }

    public String getResolvedBy() {
        return this.broadcastResponse.getResolvedBy();
    }

    public boolean asBoolean() {
        return true;
    }

    public <T> T asType(Object obj, Class<T> type) {
        throw new NotImplementedException();
    }

    public MetaProperty hasProperty(Object obj, String name) {
        return null;
    }

}
