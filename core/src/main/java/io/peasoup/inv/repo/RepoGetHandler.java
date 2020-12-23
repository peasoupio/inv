package io.peasoup.inv.repo;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Handles "get" calls from an INV context.
 * IMPORTANT: To load an actual REPO, RepoHandler is the handler.
 */
public class RepoGetHandler {

    @Getter
    private final Queue<String> sources;

    public RepoGetHandler() {
        this.sources = new ConcurrentLinkedDeque<>();
    }

    public void call(String src) {
        if (StringUtils.isEmpty(src))
            throw new IllegalArgumentException("src");

        for(String singleSrc : src.split(System.lineSeparator())) {
            if (StringUtils.isEmpty(singleSrc))
                continue;

            this.sources.add(singleSrc);
        }
    }
}
