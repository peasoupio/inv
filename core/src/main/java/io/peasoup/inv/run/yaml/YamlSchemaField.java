package io.peasoup.inv.run.yaml;

import java.util.*;

public enum YamlSchemaField {

    INV ("inv"),

    NAME ("name"),
    PATH ("path"),
    MARKDOWN ("markdown"),

    TAGS ("tags"),
    POP ("pop"),
    TAIL ("tail"),

    WORKFLOW ("workflow"),
    BROADCAST ("broadcast"),
    REQUIRE ("require"),
    STEP ("step"),

    STATEMENT_NAME ("name"),
    STATEMENT_ID ("id"),
    STATEMENT_MARKDOWN ("markdown"),

    READY_EVENT ("ready"),
    RESOLVED_EVENT ("resolved"),
    UNRESOLVED_EVENT ("unresolved"),

    WHEN ("when"),
    SCOPE ("scope"),
    TYPE ("type"),
    WHEN_ID ("id"),
    COMPLETED_EVENT ("completed"),
    CREATED_EVENT ("createD");


    private final String label;

    YamlSchemaField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static <T> T get(Map<?,?> yamlObj, YamlSchemaField...fields) {
        if (yamlObj == null)
            return null;

        if (fields == null || fields.length == 0)
            return null;

        Map<?, ?> currentObj = yamlObj;
        List<YamlSchemaField> scopes = new ArrayList<>();
        Collections.addAll(scopes, fields);

        // For each scope (field), try to reach its equivalent in the YamlObj
        while(scopes.size() > 1) {
            YamlSchemaField nextScope = scopes.remove(0);

            // If scope is null, returns null right away
            if (nextScope == null)
                return null;

            Object returnValue = currentObj.get(nextScope.label());

            // If end as not been reach, but cannot proceed (not a map value), return null right away
            if (!isMap(returnValue))
                return null;


            currentObj = (Map<?,?>)returnValue;
        }

        return (T)currentObj.get(scopes.get(0).label());
    }

    public static boolean isMap(Object yamlObj) {
        if (yamlObj == null)
            return false;

        return yamlObj instanceof Map<?, ?>;
    }
}
