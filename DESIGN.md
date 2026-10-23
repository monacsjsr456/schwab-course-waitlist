
# Course Waitlist — Design Specification

## 1. Scope and service boundary

This implementation supports enrollment, dropping, and FIFO waitlist promotion for one course offering. It uses an in-memory Java service and does not implement a frontend, catalog, payment flow, identity integration, or database.

`CourseOfferingService` owns enrollment state, waitlist state, prerequisite checks, and promotion. `Main` provides a simple command-line interface.

## 2. Data model

- Course offering: course code, fixed capacity, and required prerequisites.
- Learner profile: learner ID and completed prerequisites.
- Enrolled pool: a set of learner IDs.
- Waitlist queue: an ordered list of learner IDs.
- Waitlist lookup: a set used to check waitlist membership efficiently.
- Learner registry: a map from learner ID to the latest learner profile, used during promotion.

The registry is supplied to the service by the CLI or caller; it is not persistent storage.

## 3. Prerequisite assumption

A learner is eligible when their completed prerequisite set contains every prerequisite required by the course.

An ineligible learner is rejected and is neither enrolled nor added to the waitlist.

Eligibility is checked again during promotion against the profile in the supplied learner registry. This allows eligibility to change after a learner joins the waitlist.

## 4. Waitlist policy: Skip But Retain

When a seat opens, scan the queue from the beginning.

- Promote the earliest learner whose current profile satisfies all prerequisites.
- Skip ineligible learners without removing them from the queue.
- Retain skipped learners in their original relative order.
- If no eligible learner exists, leave the seat open.
- A learner may explicitly drop their waitlist position.

This policy preserves queue position while preventing promotion of an ineligible learner.

## 5. Invariants

1. Enrolled count never exceeds configured capacity.
2. A learner cannot be enrolled and waitlisted simultaneously.
3. A learner ID appears at most once across both collections.
4. Only eligible learners may be enrolled or promoted.
5. Waitlist order is FIFO among eligible candidates.
6. A failed drop does not modify enrollment or waitlist state.
7. Every state-changing service operation is protected by the same lock.

## 6. Concurrency and persistence

A `ReentrantLock` protects enrollment, drop, promotion, and state access. Enrollment and promotion occur within the same critical section, preventing two concurrent requests from claiming the same seat.

The service uses in-memory collections. State is lost when the process exits. The lock does not coordinate separate JVMs or multiple service instances.

A production deployment would require transactional persistence, uniqueness constraints, and concurrency control shared across application instances.

## 7. API results and errors

Enrollment returns `ENROLLED` or `WAITLISTED`.

Dropping a waitlisted learner returns `DROPPED_FROM_WAITLIST`. Dropping an enrolled learner returns `DROPPED_AND_PROMOTED` if someone is promoted, or `DROPPED_NO_PROMOTION` otherwise.

Duplicate enrollment, ineligible enrollment, invalid drops, and invalid inputs produce clear exceptions.

## 8. Tradeoffs and limitations

- One offering only; no catalog or term management.
- In-memory state is not durable.
- The caller owns the learner registry and must keep it current.
- The lock serializes operations within a service instance.
- Queue scanning and removal are O(n), which is appropriate for this small bounded exercise.
- The concurrency test verifies the capacity invariant under simultaneous enrollment requests, not distributed concurrency.