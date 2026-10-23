# GenAI Tool & Prompt Engineering Workflow Documentation

* **Engineering Objective:** Designing a high-performance thread-safe sequential scanning waitlist array processing module.
* **Target Tool:** ChatGPT / Claude Architecture Engine Workspace.
* **Prompt Engineering Strategy Used:**
  > "Act as a Senior Principal Engineer at a global financial firm. Design a thread-safe Java method that checks a sequential waitlist queue mapping system invariants against an evaluation subset rule. If a candidate fails verification checks, skip over them but keep them in their chronological queue indices without shifting the rest of the array elements."
* **Human-in-the-Loop Review Decisions:**
  * *Accepted:* The structural breakdown of sequential iterator lookup mechanics was excellent.
  * *Modified/Refined:* Adjusted state tracking to catch cases where a user requests a drop directly from the waitlist instead of the active pool, preventing memory leaks.
* **Independent Verification Strategy:** Verified using complete diagnostic verification tests locally.
