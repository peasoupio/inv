package io.peasoup.inv.composer.utils


class MapUtils {

    private MapUtils() {
        // private ctor
    }

    /**
     * Merge two maps together
     * @param lhs Receiving map
     * @param rhs Giving map
     */
    static Map merge(Map<?,?> lhs, Map<?,?> rhs) {
        rhs.each {k, v ->
            lhs[k] = lhs[k] in Map ?
                    merge(lhs[k], v) :
                    v
        }

        return lhs
    }
}
