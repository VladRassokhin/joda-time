package org.joda.time;

import com.intellij.TestDiscoveryListener;
import junit.framework.TestResult;
import junit.textui.TestRunner;

import java.util.Locale;
import java.util.TimeZone;

public class TestAllPackagesWithInstrumentation extends TestAllPackages {
    public TestAllPackagesWithInstrumentation(String testName) {
        super(testName);
    }


    public static void main(String args[]) {
        // setup a time zone other than one tester is in
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        // setup a locale other than one the tester is in
        Locale.setDefault(new Locale("th", "TH"));

        // run tests
        String[] testCaseName = {
                TestAllPackages.class.getName()
        };

        final TestDiscoveryListener listener = new TestDiscoveryListener();

        try {
            if (listener.getData() == null) {
                System.err.println("Ensure test is run with proper javaagent");
                System.exit(TestRunner.EXCEPTION_EXIT);
            }
        } catch (Exception e) {
            System.err.println("Ensure test is run with proper javaagent");
            System.exit(TestRunner.EXCEPTION_EXIT);
        }

        TestRunner runner = new TestRunner() {
            @Override
            protected TestResult createTestResult() {
                final TestResult result = super.createTestResult();
                result.addListener(listener);
                return result;
            }
        };

        int code = TestRunner.SUCCESS_EXIT;
        final long start = System.currentTimeMillis();
        try {
            TestResult r = runner.start(testCaseName);
            if (!r.wasSuccessful()) code = (TestRunner.FAILURE_EXIT);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            code = (TestRunner.EXCEPTION_EXIT);
        } finally {
            listener.close();
        }
        final long total = System.currentTimeMillis() - start;
        System.out.println("All tests took " + total + "ms to run");

        System.exit(code);
    }
}
