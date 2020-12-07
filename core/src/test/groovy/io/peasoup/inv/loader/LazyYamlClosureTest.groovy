package io.peasoup.inv.loader

import io.peasoup.inv.run.Inv
import io.peasoup.inv.run.NetworkValuablePool
import org.apache.commons.lang.NotImplementedException
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class LazyYamlClosureTest {

    Inv owner;
    LazyYamlClosure simpleClosure
    LazyYamlClosure delegateClosure

    @Before
    void setup() {
        owner = new Inv.Context(new NetworkValuablePool()).build()
        simpleClosure = new LazyYamlClosure(owner, """
            return \"it works!\"
        """)

        delegateClosure = new LazyYamlClosure(owner, """
            return \"\${value} works!\".toString()
        """)
    }

    @Test
    void invokeMethod_fail() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.invokeMethod("anything", null)
        })
    }

    @Test
    void getProperty_fail() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.getProperty("anything")
        })
    }

    @Test
    void setProperty_fail() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.setProperty("anything", "anyvalue")
        })
    }

    @Test
    void call_ok() {
        assertEquals "it works!", simpleClosure.call()

        delegateClosure.setDelegate([value: "my script"])
        assertEquals "my script works!", delegateClosure.call()
    }

    @Test
    void call_multiple_args_fail() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.call("anything", "else")
        })
    }

    @Test
    void doCall_ok() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.doCall("anything")
        })
    }

    @Test
    void delegate_ok() {
        String myNewDelegate = "my new delegate!"
        simpleClosure.setDelegate(myNewDelegate)

        assertEquals myNewDelegate, simpleClosure.getDelegate()
        assertEquals simpleClosure.getCodeClosure().getDelegate(), simpleClosure.getDelegate()
    }

    @Test
    void getParameterTypes_ok() {
        assertNotNull simpleClosure.getParameterTypes()
    }

    @Test
    void getMaximumNumberOfParameters_ok() {
        assertEquals 1, simpleClosure.getMaximumNumberOfParameters()
    }

    @Test
    void run_ok() {
        simpleClosure.run()
    }

    @Test
    void hashCode_ok() {
        assertEquals simpleClosure.getCodeClosure().hashCode(), simpleClosure.hashCode()
    }

    @Test
    void equals_ok() {
        assertTrue simpleClosure.equals(simpleClosure.getCodeClosure())
    }

    @Test
    void toString_ok() {
        assertEquals simpleClosure.getCodeClosure().toString(), simpleClosure.toString()
    }

    @Test
    void curry_fail() {
        assertThrows(NotImplementedException.class, {
            simpleClosure.curry(null)
        })

        assertThrows(NotImplementedException.class, {
            simpleClosure.curry(null, null)
        })
    }

    @Test
    void resolveStrategy_ok() {
        int resolveStategy = Closure.DONE
        simpleClosure.setResolveStrategy(resolveStategy)

        assertEquals resolveStategy, simpleClosure.getResolveStrategy()
        assertEquals simpleClosure.getCodeClosure().getResolveStrategy(), simpleClosure.getResolveStrategy()
    }
}