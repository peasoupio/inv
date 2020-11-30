package io.peasoup.inv.run

import org.codehaus.groovy.runtime.InvokerHelper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class InvNamesTest {

    @Test
    void invokeMethod() {
        def methodName = "mymethod1"
        def myId = [my: "id"]
        def expectedStatementDesc = new StatementDescriptor(methodName).call(myId)

        def actualStatementDesc = InvNames.Instance.mymethod1(myId)

        assertTrue actualStatementDesc instanceof StatementDescriptor

        assertEquals expectedStatementDesc.name, ((StatementDescriptor)actualStatementDesc).name
        assertEquals expectedStatementDesc.id, ((StatementDescriptor)actualStatementDesc).id
    }

    @Test
    void getProperty() {
        def methodName = "myproperty1"
        def expectedStatementDesc = new StatementDescriptor(methodName)

        def actualStatementDesc = InvNames.Instance.myproperty1

        assertTrue actualStatementDesc instanceof StatementDescriptor

        assertEquals expectedStatementDesc.name, ((StatementDescriptor)actualStatementDesc).name
        assertEquals expectedStatementDesc.id, ((StatementDescriptor)actualStatementDesc).id
    }

    @Test
    void invokeMethod_asBoolean() {
        assertTrue InvNames.Instance.asBoolean() as boolean
    }
}
