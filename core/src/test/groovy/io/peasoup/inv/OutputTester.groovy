package io.peasoup.inv


import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class OutputTester {

    void assertOutput(String expectedExitCode, String expectedLogFile, int actualExitCode, List actualLogs) {
        assertEquals Integer.parseInt(expectedExitCode), actualExitCode

        def expected = new File(OutputTester.getResource(expectedLogFile).path).readLines()
        def actual = new ArrayList<String>()

        // Make sure every line seperator are counted as a single line
        for (String log : actualLogs) {
            actual.addAll(log.split(System.lineSeparator()))
        }

        String actualTotal = String.join(System.lineSeparator(), actualLogs)
        println "Actual: "
        println actualTotal

        assertEquals String.join(System.lineSeparator(), actual), expected.size(), actual.size()

        for (i in 0..<expected.size()) {
            String actualStr = actual.get(i).trim()
            String expectedStr = expected.get(i).trim()
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
