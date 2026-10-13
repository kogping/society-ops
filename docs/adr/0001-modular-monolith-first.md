# 1. Modular monolith first, not microservices from day one

Status: accepted (P0)

## Context
Society Ops starts with one capability (reimbursements) and a part-time team of one. We don't yet know
where the real service boundaries are.

## Decision
One deployable (`core-service`) split into modules (`requests`, `identity`, `shared`, later `workflow`,
`audit`, ...). Each module uses hexagonal layering: `domain` (plain Java, no Spring), `application`
(use cases, ports), `adapters` (REST, JPA, ...). One service is extracted in P3 (notifications), once the
boundary has been proven in-process.

## Consequences
- No network calls, distributed transactions or deployment orchestration while the model is still changing.
- Boundaries are enforced by package structure now; Spring Modulith and ArchUnit tests will enforce them in P3.
- Extraction later costs some work, but we'd rather pay it for a boundary we know than guess one today.
