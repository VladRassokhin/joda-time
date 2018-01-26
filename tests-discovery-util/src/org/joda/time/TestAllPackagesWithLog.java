package org.joda.time;

import junit.framework.TestResult;
import junit.textui.TestRunner;

import java.util.Locale;
import java.util.TimeZone;

public class TestAllPackagesWithLog extends TestAllPackages {
    public TestAllPackagesWithLog(String testName) {
        super(testName);
    }


    public static void main(String args[]) {
        // setup a time zone other than one tester is in
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        // setup a locale other than one the tester is in
        Locale.setDefault(new Locale("th", "TH"));

        // run tests

        TestRunner runner = new TestRunner();
        int code = TestRunner.SUCCESS_EXIT;
        final long start = System.currentTimeMillis();
        try {
            TestResult r = runner.start(new String[]{
                    TestAllPackages.class.getName()
            });
            if (!r.wasSuccessful()) code = (TestRunner.FAILURE_EXIT);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            code = (TestRunner.EXCEPTION_EXIT);
        }
        final long total = System.currentTimeMillis() - start;
        System.out.println("All tests took " + total + "ms to run");

        System.exit(code);
    }
}
