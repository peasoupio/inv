package io.peasoup.inv.loader

import org.apache.commons.lang.NotImplementedException
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class InterpolableGroovyObjectTest {

    Object groovyObj
    InterpolableGroovyObject interpolable

    @Before
    void setup() {
        groovyObj = new InterpolableTestObject()
        interpolable = new InterpolableGroovyObject(groovyObj)
    }

    @Test
    void size_ok() {
        interpolable.size()
    }

    @Test
    void isEmpty_ok() {
        assertFalse interpolable.isEmpty()
    }

    @Test
    void containsKey_ok() {
        assertTrue interpolable.containsKey("my")
        assertFalse interpolable.containsKey("key")
    }

    @Test
    void containsValue_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.containsValue(null)
        })
    }

    @Test
    void get_ok() {
        assertEquals "value", interpolable.get("my")
    }

    @Test
    void get_fail() {
        assertThrows(MissingPropertyException.class, {
            interpolable.get("not-existing")
        })
    }

    @Test
    void put_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.put(null, null)
        })
    }

    @Test
    void remove_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.remove(null)
        })
    }

    @Test
    void putAll_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.putAll((Map<? extends String, ? extends String>) null)
        })
    }

    @Test
    void clear_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.clear()
        })
    }

    @Test
    void keySet_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.keySet()
        })
    }

    @Test
    void values_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.values()
        })
    }

    @Test
    void entrySet_fail() {
        assertThrows(NotImplementedException.class, {
            interpolable.entrySet()
        })
    }

    class InterpolableTestObject {
        String my = "value"
    }
}
