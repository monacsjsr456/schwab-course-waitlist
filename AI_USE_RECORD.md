
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

[Replace this section with the exact prompt actually submitted to the AI tool.]

Purpose:
Review the enrollment and waitlist tests against acceptance criteria AC1–AC7, identify tests that do not exercise the intended behavior, and propose focused corrections.

AI suggestion:
[Record the relevant response summary.]

Decision:
[State which suggestions were accepted, modified, or rejected and why.]

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
- Java version: [record `java -version` output]
- Maven version: [record `mvn -version` output]

Commands:
1. `mvn clean test`
2. [Record any additional command actually executed.]

Test results:
[Paste the actual Maven test summary and test count.]

Manual review:
[Record the specific code paths and invariants inspected.]

## 5. Limitations

The implementation is in-memory and supports one course offering in one JVM. It does not provide durable persistence or distributed locking.

## 6. Integrity statement

AI suggestions were reviewed rather than accepted blindly. The final record distinguishes suggestions, changes actually made, and verification that was actually executed. No credentials or secrets were included in prompts.