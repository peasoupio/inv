package io.peasoup.inv.scm

import org.junit.Test

import static junit.framework.Assert.assertEquals

class ScmDescriptorTest  {

    @Test
    void ok() {

        def testScm = ScmDescriptorTest.class.getResource("/test-scm.groovy")
        def scmDescriptor = new ScmDescriptor(testScm.newReader())

        assert scmDescriptor.scms()["my-repository"]

        assertEquals scmDescriptor.scms()["my-repository"].src, "https://github.com/spring-guides/gs-spring-boot.git"
        assertEquals scmDescriptor.scms()["my-repository"].entry, "inv.groovy"
        assertEquals scmDescriptor.scms()["my-repository"].timeout, 30000

        assert scmDescriptor.scms()["my-repository"].hooks
        assert scmDescriptor.scms()["my-repository"].hooks.init.contains("mkdir my-repository")
        assert scmDescriptor.scms()["my-repository"].hooks.update.contains("echo 'update'")
    }
}
