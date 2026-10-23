
package com.schwab.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class CourseOfferingService {

    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {
            super(message);
        }
    }

    public static class IneligibleLearnerException extends RegistrationException {
        public IneligibleLearnerException(String message) {
            super(message);
        }
    }

    public static class DuplicateEnrollmentException extends RegistrationException {
        public DuplicateEnrollmentException(String message) {
            super(message);
        }
    }

    public static class InvalidDropException extends RegistrationException {
        public InvalidDropException(String message) {
            super(message);
        }
    }

    public static class LearnerProfile {
        private final String learnerId;
        private final Set<String> completedPrerequisites;

        public LearnerProfile(String learnerId, Set<String> completedPrerequisites) {
            if (learnerId == null || learnerId.isBlank()) {
                throw new IllegalArgumentException("Learner ID cannot be blank.");
            }
            if (completedPrerequisites == null) {
                throw new IllegalArgumentException(
                    "Completed prerequisites cannot be null."
                );
            }

            this.learnerId = learnerId.trim();
            this.completedPrerequisites = Collections.unmodifiableSet(
                new HashSet<>(completedPrerequisites)
            );
        }

        public String getLearnerId() {
            return learnerId;
        }

        public Set<String> getCompletedPrerequisites() {
            return completedPrerequisites;
        }
    }

    private final String courseCode;
    private final int capacity;
    private final Set<String> prerequisites;

    private final ReentrantLock lock = new ReentrantLock();

    private final Set<String> enrolledPool = new HashSet<>();
    private final LinkedList<String> waitlistQueue = new LinkedList<>();
    private final Set<String> waitlistLookup = new HashSet<>();

    public CourseOfferingService(
            String courseCode,
            int capacity,
            Set<String> prerequisites) {

        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be blank.");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException(
                "Capacity cannot be negative."
            );
        }
        if (prerequisites == null) {
            throw new IllegalArgumentException(
                "Prerequisites cannot be null."
            );
        }

        this.courseCode = courseCode.trim();
        this.capacity = capacity;
        this.prerequisites = Collections.unmodifiableSet(
            new HashSet<>(prerequisites)
        );
    }

    /**
     * Enrolls an eligible learner if a seat is available.
     * Otherwise, appends the learner to the FIFO waitlist.
     */
    public String enroll(LearnerProfile learner) {
        if (learner == null) {
            throw new IllegalArgumentException("Learner cannot be null.");
        }

        lock.lock();
        try {
            String id = learner.getLearnerId();

            if (enrolledPool.contains(id) || waitlistLookup.contains(id)) {
                throw new DuplicateEnrollmentException(
                    "Learner is already enrolled or waitlisted."
                );
            }

            if (!learner.getCompletedPrerequisites()
                    .containsAll(prerequisites)) {
                throw new IneligibleLearnerException(
                    "Learner lacks required prerequisites."
                );
            }

            if (enrolledPool.size() < capacity) {
                enrolledPool.add(id);
                return "ENROLLED";
            }

            waitlistQueue.addLast(id);
            waitlistLookup.add(id);
            return "WAITLISTED";
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drops an enrolled learner and promotes the earliest eligible
     * waitlisted learner, skipping but retaining ineligible learners.
     *
     * A learner may also drop their own waitlist entry.
     */
    public String drop(
            String learnerId,
            Map<String, LearnerProfile> globalRegistry) {

        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException(
                "Learner ID cannot be blank."
            );
        }
        if (globalRegistry == null) {
            throw new IllegalArgumentException(
                "Learner registry cannot be null."
            );
        }

        String id = learnerId.trim();

        lock.lock();
        try {
            if (waitlistLookup.contains(id)) {
                waitlistQueue.remove(id);
                waitlistLookup.remove(id);
                return "DROPPED_FROM_WAITLIST";
            }

            if (!enrolledPool.contains(id)) {
                throw new InvalidDropException(
                    "Learner is not enrolled or waitlisted."
                );
            }

            enrolledPool.remove(id);

            boolean promoted = promoteNextEligible(globalRegistry);

            return promoted ? "DROPPED_AND_PROMOTED"
                            : "DROPPED_NO_PROMOTION";
        } finally {
            lock.unlock();
        }
    }

    /**
     * Policy: Skip But Retain.
     * An ineligible learner remains in their original queue position.
     * The earliest eligible learner is promoted.
     */
    private boolean promoteNextEligible(
            Map<String, LearnerProfile> globalRegistry) {

        String candidateToPromote = null;

        for (String candidateId : waitlistQueue) {
            LearnerProfile profile = globalRegistry.get(candidateId);

            if (profile != null
                    && profile.getCompletedPrerequisites()
                        .containsAll(prerequisites)) {
                candidateToPromote = candidateId;
                break;
            }
        }

        if (candidateToPromote == null) {
            return false;
        }

        waitlistQueue.remove(candidateToPromote);
        waitlistLookup.remove(candidateToPromote);
        enrolledPool.add(candidateToPromote);
        return true;
    }

    public int getEnrolledCount() {
        lock.lock();
        try {
            return enrolledPool.size();
        } finally {
            lock.unlock();
        }
    }

    public int getWaitlistCount() {
        lock.lock();
        try {
            return waitlistQueue.size();
        } finally {
            lock.unlock();
        }
    }

    public List<String> getWaitlistOrder() {
        lock.lock();
        try {
            return Collections.unmodifiableList(
                new ArrayList<>(waitlistQueue)
            );
        } finally {
            lock.unlock();
        }
    }

    public boolean isEnrolled(String learnerId) {
        lock.lock();
        try {
            return enrolledPool.contains(learnerId);
        } finally {
            lock.unlock();
        }
    }

    public boolean isWaitlisted(String learnerId) {
        lock.lock();
        try {
            return waitlistLookup.contains(learnerId);
        } finally {
            lock.unlock();
        }
    }

    public String getCourseCode() {
        return courseCode;
    }

    public int getCapacity() {
        return capacity;
    }
}