package io.peasoup.inv.run.yaml

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class YamlRequireStatementDescriptorTest {

    @Test
    void getName() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setName("name")
        assertEquals "name", yaml.getName()
    }

    @Test
    void getId() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setId([id: 1])
        assertEquals ([id: 1], yaml.getId())
    }

    @Test
    void getMarkdown() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setMarkdown("markdown")
        assertEquals "markdown", yaml.getMarkdown()
    }

    @Test
    void getResolved() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setResolved("resolved")
        assertEquals "resolved", yaml.getResolved()
    }

    @Test
    void getUnresolved() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setUnresolved("unresolved")
        assertEquals "unresolved", yaml.getUnresolved()
    }

    @Test
    void getUnbloatable() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setUnbloatable(true)
        assertTrue yaml.getUnbloatable()
    }

    @Test
    void getDefaults() {
        def yaml = new YamlRequireStatementDescriptor()
        yaml.setDefaults(true)
        assertTrue yaml.getDefaults()
    }
}
