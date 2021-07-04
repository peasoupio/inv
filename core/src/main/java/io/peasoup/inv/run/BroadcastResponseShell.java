package io.peasoup.inv.run;

import groovy.lang.*;
import lombok.Getter;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException;

import java.util.Map;

@Getter
public class BroadcastResponseShell {

    private final Object dynamicResponse;

    private final boolean readOnly;

    public BroadcastResponseShell(BroadcastResponse broadcastResponse, MetaClass dynamicResponseMetaClass, Object dynamicResponse) {
        if (broadcastResponse == null)
            throw new IllegalArgumentException("broadcastResponse");

        if (dynamicResponseMetaClass == null)
            throw new IllegalArgumentException("dynamicResponseMetaClass");

        if (dynamicResponse == null)
            throw new IllegalArgumentException("dynamicResponse");

        Tuple2<Boolean, Object> shell = createShell(broadcastResponse, dynamicResponseMetaClass, dynamicResponse);

        this.readOnly = !shell.getV1();
        this.dynamicResponse = shell.getV2();
    }

    /**
     * Create a new shell object which will be used as a proxy for getter, setter, invocations, etc.
     *
     * @param broadcastResponse The BroadcastResponse
     * @param dynamicResponseMetaClass The dyanmic response MetaClass
     * @param dynamicResponse The dyanmic response
     *
     * @return True if writable, otherwise false and a copy of BroadcastResponse.response
     */
    private Tuple2<Boolean, Object> createShell(BroadcastResponse broadcastResponse, MetaClass dynamicResponseMetaClass, Object dynamicResponse) {

        // Create actual object
        Tuple2<Boolean, Object> copiedResponse = copyResponse(broadcastResponse, dynamicResponse);
        Object newShell = copiedResponse.getV2();

        // Enhance copied response only if a new instance using a proper constructor has been used
        if (copiedResponse.getV1()) {

            if (newShell instanceof Map) {
                ((Map) newShell).putAll((Map) dynamicResponse);
            }

            if (newShell instanceof GroovyObject) {
                for (MetaProperty mp : dynamicResponseMetaClass.getProperties()) {
                    if (mp instanceof MetaBeanProperty) {
                        MetaBeanProperty mbp = (MetaBeanProperty) mp;

                        // Do not proceed with read only values
                        if (mbp.getSetter() == null)
                            continue;

                        dynamicResponseMetaClass.setProperty(
                                newShell,
                                mbp.getName(),
                                mbp.getProperty(dynamicResponse));
                    }
                }
            }
        }

        return copiedResponse;
    }

    /**
     * Copy a dynamic response object by raising its class empty constructor, if available.
     * Otherwise, add the response in a map using "value" as its key.
     *
     * @param broadcastResponse The BroadcastResponse
     * @param dynamicResponse The dynamic response
     *
     * @return True if a constructor has been used, otherwise false and a Copy of BroadcastResponse
     */
    private Tuple2<Boolean, Object> copyResponse(BroadcastResponse broadcastResponse, Object dynamicResponse) {
        try {
            return new Tuple2<>(true, InvokerHelper.invokeNoArgumentsConstructorOf(dynamicResponse.getClass()));
        } catch (MethodSelectionException mse) {
            return new Tuple2<>(false,  Map.of("value", dynamicResponse));
        }
    }
}
