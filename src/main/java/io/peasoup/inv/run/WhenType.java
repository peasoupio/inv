package io.peasoup.inv.run;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WhenType {

    private final WhenData data;

    public WhenType(WhenData data) {
        if (data == null) {
            throw new IllegalArgumentException("Data is required");
        }

        this.data = data;
    }

    public WhenEvent name(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        data.setProcessor(new NameProcessor(data));
        data.setValue(name);

        return new WhenEvent(data);
    }

    public WhenEvent tags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            throw new IllegalArgumentException("Tags is required and not empty");
        }

        data.setProcessor(new TagsProcessor(data));
        data.setValue(tags);

        return new WhenEvent(data);
    }

    interface TypeProcessor {
        boolean qualify(NetworkValuablePool pool, Inv inv);
        int qualifyState(NetworkValuablePool pool, Inv inv);
    }

    static final class NameProcessor implements TypeProcessor {
        private final WhenData whenData;

        public NameProcessor(WhenData whenData) {
            if (whenData == null) {
                throw new IllegalArgumentException("WhenData is required");
            }

            this.whenData = whenData;
        }

        @Override
        public boolean qualify(NetworkValuablePool pool, Inv inv) {
            return qualifyState(pool, inv) > 0;
        }

        @Override
        public int qualifyState(NetworkValuablePool pool, Inv inv) {
            // Do not process until ALL statements are managed
            if (!inv.getRemainingStatements().isEmpty())
                return -1;

            String whenStringValue = (String)whenData.getValue();

            // Attempt to get other matching name
            Inv other = pool.getTotalInvs().stream()
                    .filter(i -> i.getName().contains(whenStringValue))
                    .findFirst()
                    .orElse(null);

            // Did not find, so skip
            if (other == null || other == inv)
                return -2;

            // If we look only when created, raise right now
            if (whenData.getEvent() == WhenEvent.Events.Created)
                return 1;


            // Otherwise, make sure it's not remaining, thus not completed
            if (whenData.getEvent() == WhenEvent.Events.Completed &&
                !pool.getRemainingInvs().contains(other))
                return 2;

            // By default, return false
            return 0;
        }

        @Override
        public String toString() {
            return "NAME";
        }
    }

    static final class TagsProcessor implements TypeProcessor {
        private final WhenData whenData;

        public TagsProcessor(WhenData whenData) {
            if (whenData == null) {
                throw new IllegalArgumentException("WhenData is required");
            }

            this.whenData = whenData;
        }


        @Override
        public boolean qualify(NetworkValuablePool pool, Inv inv) {
            return qualifyState(pool, inv) > 0;
        }

        @Override
        public int qualifyState(NetworkValuablePool pool, Inv inv) {
            // Do not process until ALL statements are managed
            if (!inv.getRemainingStatements().isEmpty())
                return -1;

            Map<String, String> whenMapValue = (Map<String, String>)whenData.getValue();
            List<Inv> matchInvs = pool.getTotalInvs().stream()
                    .filter(i ->
                            // Make sure it has a valid tags
                            i.getTags() != null &&  !i.getTags().isEmpty() &&
                                    // Do not process same INV
                                    i != inv &&
                                    // Check if all tags from when data is included
                                    whenMapValue.equals(DefaultGroovyMethods.intersect(i.getTags(), whenMapValue)))
                    .collect(Collectors.toList());

            // If nothing was matched, skip
            if (matchInvs.isEmpty())
                return -2;

            // If we look only when created, raise right now
            if (whenData.getEvent() == WhenEvent.Events.Created)
                return 1;

            // Otherwise, make sure it's not remaining, thus not completed
            if (whenData.getEvent() == WhenEvent.Events.Completed) {

                Logger.debug("Check match results for " + whenData.toString());

                boolean allCompleted = true;
                for(Inv matchInv : matchInvs) {
                    boolean completed = !pool.getRemainingInvs().contains(matchInv) &&
                                        pool.getTotalInvs().contains(matchInv);

                    Logger.debug("\t" + matchInv.getName() + " is " + (completed? "completed": "not completed"));

                    if (!completed)
                        allCompleted = false;
                }

                // If all matched INVS are completed
                if (allCompleted)
                    return 2;
            }

            // By default, return false
            return 0;
        }

        @Override
        public String toString() {
            return "TAGS";
        }
    }
}
