package io.peasoup.inv.repo;

import io.peasoup.inv.Logger;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Handles "get" calls from an INV context.
 * IMPORTANT: To load an actual REPO, RepoHandler is the handler.
 */
public class RepoLoadHandler {

    @Getter
    private final Queue<String> sources;

    public RepoLoadHandler() {
        this.sources = new ConcurrentLinkedDeque<>();
    }

    public void call(String src) {
        if (StringUtils.isEmpty(src))
            throw new IllegalArgumentException("src");

        for(String singleSrc : src.split(System.lineSeparator())) {
            if (StringUtils.isEmpty(singleSrc))
                continue;

            if (!UrlValidator.getInstance().isValid(singleSrc))
                Logger.warn(singleSrc + " is not a valid URL");

            this.sources.add(singleSrc);
        }
    }
}
