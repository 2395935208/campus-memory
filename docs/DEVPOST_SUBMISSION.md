# Devpost submission draft

## Project name

CampusMemory — a transparent memory layer for learning

## Submission links

- Live application: [http://47.239.40.162](http://47.239.40.162)
- Public health check: [http://47.239.40.162/api/health](http://47.239.40.162/api/health)
- Public GitHub repository: [https://github.com/2395935208/campus-memory](https://github.com/2395935208/campus-memory)
- Architecture diagram: [`docs/ARCHITECTURE.md`](https://github.com/2395935208/campus-memory/blob/main/docs/ARCHITECTURE.md)
- Alibaba Cloud deployment proof: [`deploy/aliyun-ecs-user-data.sh`](https://github.com/2395935208/campus-memory/blob/main/deploy/aliyun-ecs-user-data.sh)

## Elevator pitch

CampusMemory is a Qwen-powered learning coach that remembers a student's goals, constraints, skill level, and recurring mistakes across sessions while keeping every memory visible and controllable.

## Inspiration

Students use AI repeatedly, but stateless assistants make them re-explain their background and can silently carry stale assumptions. We wanted a learning coach that gets more useful over time without turning memory into a black box.

## What it does

CampusMemory uses Qwen Cloud structured output to extract durable facts from natural conversation. A memory lifecycle service normalizes stable keys, replaces conflicting facts, expires temporary context, and retrieves a bounded top-five context using importance, keyword relevance, and recency. The learner sees both the memories used for each answer and a complete vault where active facts can be forgotten.

## How we built it

The application is a Java 17 Spring Boot monolith with a static responsive web interface, REST API, JPA, and persistent H2 storage. Qwen Cloud is called through its OpenAI-compatible Chat Completions endpoint for structured extraction and personalized coaching. The app is packaged as a Docker container and deployed on Alibaba Cloud ECS in the China (Hong Kong) region with a persistent data volume.

## Challenges

The central challenge was deciding what deserves long-term storage. We separated durable learner facts from casual conversation, introduced stable memory keys for conflict resolution, and made retrieval scores visible so memory behavior can be inspected rather than guessed.

## Accomplishments

- Cross-session persistent memory with a visible evidence trail.
- Automatic replacement of stale, conflicting facts.
- TTL-based forgetting and learner-controlled forgetting.
- Bounded, explainable retrieval instead of replaying full conversation history.
- One deployable container with no external database dependency.

## What we learned

Long-term agent memory is not just storage. A useful system needs extraction policy, conflict semantics, bounded recall, expiration, transparency, and user control.

## What's next

We would add semantic embeddings, encrypted per-user storage, memory confirmation workflows, and longitudinal learning-outcome evaluation while preserving the same transparent lifecycle.

## Built with

Qwen Cloud, Alibaba Cloud ECS, Java 17, Spring Boot, Spring Data JPA, H2, Docker, HTML, CSS, and JavaScript.
