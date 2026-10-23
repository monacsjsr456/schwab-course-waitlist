
# Course Waitlist

A bounded Java implementation of course enrollment, dropping, and FIFO waitlist promotion.

## Requirements

- JDK 21
- Maven 3.9 or later

## Run tests

From the repository root:

```bash
mvn clean test
```

This compiles the Java source and runs the JUnit 5 automated tests.

## Run the CLI

Compile the project and start the command-line application:

```bash
mvn clean package
java -cp target/classes com.schwab.registration.Main
```

Enter a course code, seat capacity, and required prerequisites. Then use the interactive menu to enroll learners, drop learners, and inspect enrollment and waitlist state.

## Design and assumptions

See [DESIGN.md](DESIGN.md) for service boundaries, invariants, concurrency, prerequisites, and the Skip But Retain waitlist policy.

See [AI_USE_RECORD.md](AI_USE_RECORD.md) for prompts, engineering decisions, and verification evidence.

See [Course-Registration-Rubric-Score.md](Course-Registration-Rubric-Score.md) for the assessment scorecard.