
package com.schwab.registration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CourseOfferingTest {

    private static final Set<String> PREREQUISITES = Set.of("CS101");

    private CourseOfferingService.LearnerProfile learner(
            String id, String... completed) {
        return new CourseOfferingService.LearnerProfile(
            id, Set.of(completed)
        );
    }

    @Test
    void enrollsEligibleLearnerWhenCapacityIsAvailable() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 2, PREREQUISITES);

        assertEquals("ENROLLED", service.enroll(learner("L1", "CS101")));
        assertTrue(service.isEnrolled("L1"));
        assertEquals(1, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void fullCourseAddsEligibleLearnerToWaitlistOnce() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        service.enroll(learner("L1", "CS101"));
        assertEquals("WAITLISTED",
            service.enroll(learner("L2", "CS101")));

        assertThrows(
            CourseOfferingService.DuplicateEnrollmentException.class,
            () -> service.enroll(learner("L2", "CS101"))
        );

        assertEquals(1, service.getEnrolledCount());
        assertEquals(List.of("L2"), service.getWaitlistOrder());
        assertEquals(1, service.getWaitlistCount());
    }

    @Test
    void rejectsIneligibleLearnerWithoutChangingState() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        assertThrows(
            CourseOfferingService.IneligibleLearnerException.class,
            () -> service.enroll(learner("BAD"))
        );

        assertEquals(0, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void droppingEnrolledLearnerPromotesEarliestEligibleAndRetainsIneligible() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        Map<String, CourseOfferingService.LearnerProfile> registry =
            new HashMap<>();

        var active = learner("ACTIVE", "CS101");
        var firstWaitlisted = learner("WAIT1", "CS101");
        var secondWaitlisted = learner("WAIT2", "CS101");

        registry.put("ACTIVE", active);
        registry.put("WAIT1", firstWaitlisted);
        registry.put("WAIT2", secondWaitlisted);

        service.enroll(active);
        assertEquals("WAITLISTED", service.enroll(firstWaitlisted));
        assertEquals("WAITLISTED", service.enroll(secondWaitlisted));

        // Simulate prerequisite eligibility changing after waitlisting.
        registry.put("WAIT1", learner("WAIT1"));

        assertEquals("DROPPED_AND_PROMOTED",
            service.drop("ACTIVE", registry));

        assertFalse(service.isEnrolled("ACTIVE"));
        assertFalse(service.isEnrolled("WAIT1"));
        assertTrue(service.isEnrolled("WAIT2"));

        // The ineligible learner is skipped but retains their queue position.
        assertEquals(List.of("WAIT1"), service.getWaitlistOrder());
        assertEquals(1, service.getEnrolledCount());
        assertEquals(1, service.getWaitlistCount());
    }

    @Test
    void duplicateEnrollmentOfAlreadyEnrolledLearnerIsRejected() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 2, PREREQUISITES);

        service.enroll(learner("L1", "CS101"));

        assertThrows(
            CourseOfferingService.DuplicateEnrollmentException.class,
            () -> service.enroll(learner("L1", "CS101"))
        );

        assertEquals(1, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void droppingWaitlistedLearnerRemovesOnlyTheirQueueEntry() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        Map<String, CourseOfferingService.LearnerProfile> registry =
            new HashMap<>();

        var active = learner("ACTIVE", "CS101");
        var waiting = learner("WAIT", "CS101");
        registry.put("ACTIVE", active);
        registry.put("WAIT", waiting);

        service.enroll(active);
        service.enroll(waiting);

        assertEquals("DROPPED_FROM_WAITLIST",
            service.drop("WAIT", registry));

        assertEquals(1, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
        assertTrue(service.isEnrolled("ACTIVE"));
    }

    @Test
    void repeatedDropDoesNotChangeState() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        Map<String, CourseOfferingService.LearnerProfile> registry =
            new HashMap<>();

        var active = learner("ACTIVE", "CS101");
        registry.put("ACTIVE", active);
        service.enroll(active);

        assertEquals("DROPPED_NO_PROMOTION",
            service.drop("ACTIVE", registry));

        assertThrows(
            CourseOfferingService.InvalidDropException.class,
            () -> service.drop("ACTIVE", registry)
        );

        assertEquals(0, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void unknownDropDoesNotChangeState() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, PREREQUISITES);

        assertThrows(
            CourseOfferingService.InvalidDropException.class,
            () -> service.drop("UNKNOWN", new HashMap<>())
        );

        assertEquals(0, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void rejectsInvalidConstructorAndLearnerInputs() {
        assertThrows(IllegalArgumentException.class,
            () -> new CourseOfferingService("", 1, Set.of()));

        assertThrows(IllegalArgumentException.class,
            () -> new CourseOfferingService("CS102", -1, Set.of()));

        assertThrows(IllegalArgumentException.class,
            () -> new CourseOfferingService("CS102", 1, null));

        assertThrows(IllegalArgumentException.class,
            () -> new CourseOfferingService.LearnerProfile("", Set.of()));

        assertThrows(IllegalArgumentException.class,
            () -> new CourseOfferingService.LearnerProfile("L1", null));

        CourseOfferingService service =
            new CourseOfferingService("CS102", 1, Set.of());

        assertThrows(IllegalArgumentException.class,
            () -> service.enroll(null));

        assertThrows(IllegalArgumentException.class,
            () -> service.drop(" ", new HashMap<>()));

        assertEquals(0, service.getEnrolledCount());
        assertEquals(0, service.getWaitlistCount());
    }

    @Test
    void zeroCapacityPlacesEligibleLearnerOnWaitlist() {
        CourseOfferingService service =
            new CourseOfferingService("CS102", 0, Set.of());

        assertEquals("WAITLISTED", service.enroll(learner("L1")));

        assertEquals(0, service.getEnrolledCount());
        assertEquals(List.of("L1"), service.getWaitlistOrder());
    }

    @Test
    void concurrentEnrollmentsNeverExceedCapacity() throws Exception {
        int capacity = 5;
        int numberOfLearners = 30;

        CourseOfferingService service =
            new CourseOfferingService("CS102", capacity, Set.of());

        ExecutorService executor =
            Executors.newFixedThreadPool(numberOfLearners);
        CountDownLatch ready = new CountDownLatch(numberOfLearners);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < numberOfLearners; i++) {
                final int learnerNumber = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return service.enroll(learner("L" + learnerNumber));
                }));
            }

            ready.await();
            start.countDown();

            int enrolledResults = 0;
            int waitlistedResults = 0;

            for (Future<String> future : futures) {
                String result = future.get();
                if ("ENROLLED".equals(result)) {
                    enrolledResults++;
                } else if ("WAITLISTED".equals(result)) {
                    waitlistedResults++;
                } else {
                    fail("Unexpected enrollment result: " + result);
                }
            }

            assertEquals(capacity, enrolledResults);
            assertEquals(numberOfLearners - capacity, waitlistedResults);
            assertEquals(capacity, service.getEnrolledCount());
            assertEquals(numberOfLearners - capacity,
                service.getWaitlistCount());
            assertEquals(numberOfLearners,
                service.getEnrolledCount() + service.getWaitlistCount());
        } finally {
            executor.shutdownNow();
        }
    }
}
