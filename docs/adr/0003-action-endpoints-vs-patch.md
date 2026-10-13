# 3. Action sub-resources instead of PATCH on status

Status: accepted (P0)

## Context
Approving, rejecting and paying are business operations with different inputs (a rejection needs a reason),
different permissions (treasurer only) and different audit meaning.

## Decision
`POST /api/v1/requests/{id}/approve | /reject | /pay`, rather than `PATCH /requests/{id} {"status": ...}`.

## Consequences
- Each action has its own request body, validation, authorisation rule and OpenAPI entry.
- Clients can't attempt arbitrary status writes; the legal lifecycle lives in the domain, and an illegal
  move returns `409` Problem Details.
- Less "RESTful purity", but the API reads like the business process. Idempotency keys and `If-Match` (P1)
  attach naturally to each action.
