package io.peasoup.inv.run.yaml

import org.junit.Test

import static org.junit.Assert.assertEquals

class YamlBroadcastStatementDescriptorTest {

    @Test
    void getName() {
        def yaml = new YamlBroadcastStatementDescriptor()
        yaml.setName("name")
        assertEquals "name", yaml.getName()
    }

    @Test
    void getId() {
        def yaml = new YamlBroadcastStatementDescriptor()
        yaml.setId([id: 1])
        assertEquals ([id: 1], yaml.getId())
    }

    @Test
    void getMarkdown() {
        def yaml = new YamlBroadcastStatementDescriptor()
        yaml.setMarkdown("markdown")
        assertEquals "markdown", yaml.getMarkdown()
    }

    @Test
    void getReady() {
        def yaml = new YamlBroadcastStatementDescriptor()
        yaml.setReady("ready")
        assertEquals "ready", yaml.getReady()
    }
}
