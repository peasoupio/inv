package io.peasoup.inv.loader

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class PackageDiscovererTest {

    @Test
    void ok() {
        String packageFileEquivalent = "my/superb/newpackage/"
        String expectedPackage = "my.superb.newpackage"

        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/src/somefile.groovy"))

        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/inv.groovy"))
        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/inv.yml"))
        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/inv.yaml"))

        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/scm.groovy"))
        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/scm.yml"))
        assertEquals expectedPackage, PackageDiscoverer.forFile(new File(packageFileEquivalent + ".inv/vars/scm.yaml"))
    }

    @Test
    void not_ok() {
        assertNull PackageDiscoverer.forFile(null)
        assertNull PackageDiscoverer.forFile(new File("test.groovy"))
        assertNull PackageDiscoverer.forFile(new File("src/test.groovy"))
        assertNull PackageDiscoverer.forFile(new File("vars/test.groovy"))
        assertNull PackageDiscoverer.forFile(new File(".inv/src/test.groovy"))
        assertNull PackageDiscoverer.forFile(new File(".inv/vars/test.groovy"))
    }

    @Test
    void ok_2() {

    }

}
