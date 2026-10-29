
# AI-Assisted Engineering Use Record

## 1. Engineering task

Implement a bounded Java service for course enrollment, dropping, and FIFO waitlist promotion, with prerequisite validation and concurrency-safe state transitions.

## 2. AI tool and prompts

Tool used: ChatGPT

### Prompt 1 — Waitlist design

Record the actual prompt used to request a thread-safe waitlist design:

> Act as a Senior Principal Engineer at a global financial firm. Design a thread-safe Java method that checks a sequential waitlist queue mapping system invariants against an evaluation subset rule. If a candidate fails verification checks, skip over them but keep them in their chronological queue indices without shifting the rest of the array elements.

Summary of relevant AI suggestion:
- Scan the waitlist in order.
- Skip candidates who fail eligibility checks.
- Preserve the positions of ineligible learners.

Decision:
- Accepted the ordered eligibility scan.
- Applied the policy to the course waitlist rather than introducing a separate configurable policy engine.

### Prompt 2 — Test review

Review the Java course enrollment and waitlist implementation against the assessment requirements. Examine the enrollment and waitlist tests, identify cases that do not exercise the intended service behavior, and recommend focused corrections. Pay particular attention to FIFO promotion, prerequisite eligibility, duplicate enrollment, invalid drop operations, and preservation of state invariants.

Purpose:
Review the enrollment and waitlist tests against acceptance criteria AC1–AC7, identify tests that do not exercise the intended behavior, and propose focused corrections.

AI suggestion:
The FIFO test modified a list returned by getWaitlistOrder(). Since the method returns a copy, modifying the returned list does not update the service's internal queue. The test should create waitlist entries through the service API, drop an enrolled student, and verify the resulting enrollment and waitlist state.

Decision:
Accepted the recommendation to validate FIFO promotion through observable service behavior instead of modifying a returned list. The implementation and test changes must be verified against the submitted repository before being described as completed.

Files affected:
- CourseOfferingService.java
- CourseOfferingTest.java

## 3. Human review and corrections

The original FIFO test modified a list returned by `getWaitlistOrder()`. That method returns a copy, so the modification did not update the service's internal queue.

The test must instead create waitlist entries through `enroll()` and verify the queue and enrollment state after dropping the active learner.

The service and tests are reviewed for:
- Duplicate enrollment.
- Eligibility checks.
- Skip-but-retain promotion.
- State preservation after invalid drops.
- Capacity preservation under concurrent enrollment.

## 4. Independent verification evidence

Environment:
- Java version: OpenJDK, version 25.0.4, as previously reported during the assessment setup.
- Maven version: Maven installation was initially unavailable in the terminal. A successful build was subsequently reported, but the exact Maven version and test summary are not available in this record
Commands:
1. `mvn clean test`

Test results:
The project was reported as built successfully. 

Manual review:
The review focused on enrollment and drop state transitions, FIFO waitlist promotion, prerequisite eligibility, duplicate operations, and synchronization of shared state.

## 5. Limitations

The implementation is in-memory and supports one course offering in one JVM. It does not provide durable persistence or distributed locking.

## 6. Integrity statement

AI suggestions were reviewed rather than accepted blindly. The final record distinguishes suggestions, changes actually made, and verification that was actually executed. No credentials or secrets were included in prompts.
