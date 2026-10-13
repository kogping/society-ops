# 2. Separate JPA entities from domain objects

Status: accepted (P0)

## Context
`Reimbursement` is a rich aggregate: no setters, state changes only via `handle(command)`, validated value
objects (`Money`, `BankDetails`), and no framework imports. JPA wants mutable, no-arg-constructor entities
with annotations. The roadmap asks whether to annotate the domain directly or keep a separate persistence model.

## Decision
Keep them separate. `RequestEntity` (package-private, in `adapters.persistence`) is a plain data holder;
`JpaRequestRepository` maps both ways and implements the `RequestRepository` port.

Optimistic locking works through the entity: within one transaction `findById` and `save` operate on the same
managed entity, so Hibernate's `@Version` check fails the flush if another transaction committed in between
(covered by `RequestFlowIntegrationTest`).

## Consequences
- The domain stays free of Spring/JPA, and invariants can't be bypassed by the ORM.
- Cost: a mapping layer and some field duplication. Acceptable at this size.
- The domain doesn't carry the version yet. P1 (ETag / If-Match) will need to surface it; revisit then.
