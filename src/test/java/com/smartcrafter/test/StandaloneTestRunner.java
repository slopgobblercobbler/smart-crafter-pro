package com.smartcrafter.test;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Standalone JUnit 5 Test Runner for SmartCrafter Pro.
 * Enables running unit tests outside of Gradle during native compilation.
 */
public class StandaloneTestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    SmartCrafter Pro - JUnit 5 Test Suite        ");
        System.out.println("=================================================");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("com.smartcrafter"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);

        long start = System.currentTimeMillis();
        launcher.execute(request);
        long duration = System.currentTimeMillis() - start;

        TestExecutionSummary summary = listener.getSummary();
        System.out.println("\n----------------- TEST SUMMARY -----------------");
        System.out.printf("Tests Found:     %d%n", summary.getTestsFoundCount());
        System.out.printf("Tests Started:   %d%n", summary.getTestsStartedCount());
        System.out.printf("Tests Succeeded: %d%n", summary.getTestsSucceededCount());
        System.out.printf("Tests Skipped:   %d%n", summary.getTestsSkippedCount());
        System.out.printf("Tests Failed:    %d%n", summary.getTestsFailedCount());
        System.out.printf("Total Duration:  %d ms%n", duration);
        System.out.println("-------------------------------------------------");

        if (summary.getTestsFailedCount() > 0) {
            System.err.println("\nFAILED TESTS:");
            for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                System.err.println("  * " + failure.getTestIdentifier().getDisplayName() + ": " + failure.getException().getMessage());
                failure.getException().printStackTrace(System.err);
            }
            System.exit(1);
        } else {
            System.out.println("\n>>> ALL TESTS PASSED SUCCESSFULLY! <<<\n");
            System.exit(0);
        }
    }
}
