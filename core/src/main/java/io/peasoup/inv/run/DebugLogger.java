package io.peasoup.inv.run;

import io.peasoup.inv.Logger;
import org.apache.commons.lang.StringUtils;

public class DebugLogger {

    public static final DebugLogger Instance = new DebugLogger();

    private DebugLogger() {

    }

    public void call(String message) {
        if (StringUtils.isEmpty(message))
            return;

        Logger.debug(message);
    }
}
