package io.peasoup.inv.testing

import io.peasoup.inv.run.InvExecutor
import io.peasoup.inv.run.InvHandler
import io.peasoup.inv.run.InvInvoker
import io.peasoup.inv.run.PoolReport
import org.junit.Before

abstract class JUnitInvTestingBase {

    final String MY_LOC = getClass().location.path
    InvExecutor invExecutor
    PoolReport report

    boolean called

    @Before
    void setup() {
        invExecutor = new InvExecutor()
        called = false
    }

    boolean getIsOk() {
        report && report.isOk()
    }

    boolean getIsHalted() {
        report && report.isHalted()
    }

    boolean getHasExceptions() {
        report && !report.errors.isEmpty()
    }

    void sequence(String... files) {
        assert files

        assert !called, 'Only call sequence once for test method'
        called = true

        for (String file : files) {
            runInv(file)
        }

        report = invExecutor.execute()
    }

    private void runInv(String value) {
        assert value, 'Inv must be a valid non-null, non-empty value'

        File invFile = new File(value)


        if (!invFile.exists())
            invFile = new File(new File(MY_LOC).parentFile, value)

        if (!invFile.exists()) {
            URL location = this.getClass().getResource(value)
            if (location)
                invFile = new File(location.path)
        }

        if (!invFile.exists())
            assert invFile.exists(), "${value} does not exists on the filesystem"

        InvInvoker.invoke(invExecutor, invFile)
    }

}
