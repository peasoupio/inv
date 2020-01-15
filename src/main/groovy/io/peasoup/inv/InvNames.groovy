package io.peasoup.inv

class InvNames {

    final static InvNames Instance = new InvNames()

    private InvNames() {

    }

    //@Override
    Object propertyMissing(String propertyName) {
        //executor.pool.checkAvailability(propertyName)
        return new StatementDescriptor(propertyName)
    }

    //@Override
    Object methodMissing(String methodName, def args) {
        //executor.pool.checkAvailability(methodName)

        //noinspection GroovyAssignabilityCheck
        return new StatementDescriptor(methodName)(*args)
    }
}
