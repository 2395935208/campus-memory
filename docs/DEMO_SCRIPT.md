# Demo video script — target 2:35

## 0:00–0:15 — Problem

“Students repeatedly explain their goals and constraints to stateless AI. CampusMemory is a Qwen-powered coach that remembers what matters and shows the learner exactly what it stored.”

## 0:15–0:35 — Architecture

Show the README diagram. Explain that Qwen extracts structured durable facts, the Spring Boot memory service owns replacement and expiration, and only five ranked memories return to Qwen.

## 0:35–1:05 — Session one

Enter: `I am learning Java backend, preparing for an internship, and can study 1 hour per day.`

Show the new memories in the vault: learning stack, career goal, and daily study time.

## 1:05–1:35 — Cross-session recall

Click **New session** and enter: `Plan today's study.`

Show the personalized response and the memory-trace relevance scores. Point out that the session ID changed while the durable learner ID remained.

## 1:35–2:00 — Update stale memory

Enter: `I can now study 2 hours per day; the old limit is outdated.`

Show the one-hour memory marked **SUPERSEDED** and the two-hour memory active.

## 2:00–2:15 — User control

Click **Forget** on one active memory, start a new session, and show that the forgotten fact is no longer recalled.

## 2:15–2:35 — Deployment and close

Show `/api/health`, the Alibaba Cloud ECS console/public IP, and the public GitHub repository. Close with: “CampusMemory turns memory from hidden chat history into a transparent, testable agent capability.”
