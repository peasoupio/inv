package io.peasoup.inv.composer

import groovy.transform.CompileStatic

@CompileStatic
class Pagination {

    final Settings settings

    Pagination(Settings settings) {
        assert settings, 'Settings is required'

        this.settings = settings
    }

    List<Object> resolve(List element, Integer from = 0, Integer to = 0) {
        if (element.isEmpty())
            return element

        Integer resolvedFrom = from ?: 0
        resolvedFrom = resolvedFrom + 1 > element.size() ? 0 : resolvedFrom

        Integer resolvedTo = to ?: (Integer) settings.filters().defaultStep
        resolvedTo = Math.min(element.size(), resolvedFrom + resolvedTo)

        return element[resolvedFrom..resolvedTo - 1]
    }
}
