package io.peasoup.inv

import static org.junit.Assert.*

class OutputTester {

    void assertOutput(String expectedExitCode, String expectedLogFile, int actualExitCode) {
        def actualLogs = Logger.getCapture()
        assertNotNull actualLogs

        def expected = new File(OutputTester.getResource(expectedLogFile).path).readLines()
        def actual = new ArrayList<String>()

        // Make sure every line seperator are counted as a single line
        for (String log : actualLogs) {
            actual.addAll(log.split(System.lineSeparator()))
        }

        String actualTotal = String.join(System.lineSeparator(), actualLogs)
        println "Actual: "
        println actualTotal

        assertEquals Integer.parseInt(expectedExitCode), actualExitCode

        for (i in 0..<expected.size()) {
            String expectedStr = expected.get(i).trim()
            assertTrue "Out of bound. Line ${i}: ${expectedStr}".toString(), i < actual.size()

            String actualStr = actual.get(i).trim()

            // Make sure expected string is regex compliant
            expectedStr = expectedStr
                    .replace("\\", "\\\\")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("{", "\\{")
                    .replace("}", "\\}")
                    .replace("^", "\\^")
                    .replace(".", "\\.")
                    .replace("\\.*", ".*")

            assertTrue(
                    (i + 1) + ": " + actualStr + " != " + expectedStr, // echo what's wrong
                    actualStr ==~ expectedStr)
        }
    }
}
