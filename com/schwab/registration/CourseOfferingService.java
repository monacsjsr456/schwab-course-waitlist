package com.schwab.registration;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class CourseOfferingService {

    // Custom Domain Exceptions
    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) { super(message); }
    }
    public static class IneligibleLearnerException extends RegistrationException {
        public IneligibleLearnerException(String message) { super(message); }
    }
    public static class DuplicateEnrollmentException extends RegistrationException {
        public DuplicateEnrollmentException(String message) { super(message); }
    }
    public static class InvalidDropException extends RegistrationException {
        public InvalidDropException(String message) { super(message); }
    }

    // Domain Models
    public static class LearnerProfile {
        private final String learnerId;
        private final Set<String> completedPrerequisites;

        public LearnerProfile(String learnerId, Set<String> completedPrerequisites) {
            this.learnerId = learnerId;
            this.completedPrerequisites = new HashSet<>(completedPrerequisites);
        }

        public String getLearnerId() { return learnerId; }
        public Set<String> getCompletedPrerequisites() { return completedPrerequisites; }
    }

    // Core Properties
    private final String courseCode;
    private final int capacity;
    private final Set<String> prerequisites;

    // Thread Safety Guard for Schwab Enterprise Concurrency Compliance
    private final ReentrantLock lock = new ReentrantLock();

    // State Stores
    private final Set<String> enrolledPool = new HashSet<>();
    private final List<String> waitlistQueue = new LinkedList<>();
    private final Set<String> waitlistLookup = new HashSet<>();

    public CourseOfferingService(String courseCode, int capacity, Set<String> prerequisites) {
        if (capacity < 0) throw new IllegalArgumentException("Capacity cannot be negative.");
        this.courseCode = courseCode;
        this.capacity = capacity;
        this.prerequisites = new HashSet<>(prerequisites);
    }

    /**
     * Enrolls a learner or appends to FIFO Waitlist. Satisfies AC 1, AC 2, AC 4.
     */
    public String enroll(LearnerProfile learner) {
        lock.lock();
        try {
            // Guard 1: Prerequisite Check (AC 4)
            if (!learner.getCompletedPrerequisites().containsAll(this.prerequisites)) {
                throw new IneligibleLearnerException("Learner lacks required prerequisites.");
            }

            // Guard 2: Prevent Duplicates Across Both Pools (AC 2)
            String id = learner.getLearnerId();
            if (enrolledPool.contains(id) || waitlistLookup.contains(id)) {
                throw new DuplicateEnrollmentException("Learner is already registered or waitlisted.");
            }

            // Evaluation 3: Active Space Allocation (AC 1)
            if (enrolledPool.size() < capacity) {
                enrolledPool.add(id);
                return "ENROLLED";
            }

            // Evaluation 4: Route to Waitlist Queue (AC 2)
            waitlistQueue.add(id);
            waitlistLookup.add(id);
            return "WAITLISTED";
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drops a learner and initiates cascading FIFO check. Satisfies AC 3, AC 4, AC 5.
     */
    public String drop(String learnerId, Map<String, LearnerProfile> globalRegistry) {
        lock.lock();
        try {
            // Guard 1: Invalid Drop Request (AC 5)
            if (!enrolledPool.contains(learnerId) && !waitlistLookup.contains(learnerId)) {
                throw new InvalidDropException("Learner is not actively tracked in this offering.");
            }

            // Case A: Learner manually leaves from inside the waitlist queue
            if (waitlistLookup.contains(learnerId)) {
                waitlistQueue.remove(learnerId);
                waitlistLookup.remove(learnerId);
                return "DROPPED_FROM_WAITLIST";
            }

            // Case B: Learner leaves active seat -> Triggers Waitlist Cascade Scan (AC 3)
            enrolledPool.remove(learnerId);
            promoteNextEligible(globalRegistry);
            return "DROPPED_AND_PROMOTED";
        } finally {
            lock.unlock();
        }
    }

    /**
     * Policy: "Skip But Retain" FIFO Evaluation Loop. Satisfies AC 3 and AC 4.
     */
    private void promoteNextEligible(Map<String, LearnerProfile> globalRegistry) {
        String promotedCandidate = null;

        for (String candidateId : waitlistQueue) {
            LearnerProfile profile = globalRegistry.get(candidateId);
            // Check real-time eligibility at execution moment
            if (profile != null && profile.getCompletedPrerequisites().containsAll(this.prerequisites)) {
                promotedCandidate = candidateId;
                break; // Found earliest matching item
            }
        }

        if (promotedCandidate != null) {
            waitlistQueue.remove(promotedCandidate);
            waitlistLookup.remove(promotedCandidate);
            enrolledPool.add(promotedCandidate);
        }
    }

    // Thread-safe Diagnostic Read Accessors
    public int getEnrolledCount() { lock.lock(); try { return enrolledPool.size(); } finally { lock.unlock(); } }
    public int getWaitlistCount() { lock.lock(); try { return waitlistQueue.size(); } finally { lock.unlock(); } }
    public List<String> getWaitlistOrder() { lock.lock(); try { return new ArrayList<>(waitlistQueue); } finally { lock.unlock(); } }
}

