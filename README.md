![CI](https://github.com/kogping/society-ops/actions/workflows/ci.yml/badge.svg)

# Society Ops

Workflow tooling for a student society: members submit requests, the treasurer approves and pays them, and
everything is tracked. **P0 (this milestone): reimbursements end to end**, modelled on the society's real
reimbursement form (claimant, bank details, portfolio, event, amount in AUD inc. GST, signed declaration).

Lifecycle: `SUBMITTED → APPROVED → PAID`, or `SUBMITTED → REJECTED` (with a reason).

## Run it locally

Prerequisites: Docker, Java 25, Node 24 (see `.sdkmanrc`, `web/.nvmrc`).

```sh
cp .env.example .env        # then edit: set MEMBER_PASSWORD and TREASURER_PASSWORD
docker compose up -d        # Postgres
./gradlew :core-service:bootRun    # API on http://localhost:8080 (profile "local", Flyway migrates on start)
cd web && npm ci && npm run dev    # UI on http://localhost:5173, proxies /api to :8080
```

Log in as `member` or `treasurer` with the passwords from `.env`. (Temporary in-memory users; Keycloak replaces
them in P1.) The member submits a claim under **New claim**; the treasurer sees it under **Treasurer queue**
and approves/rejects, then marks it paid.

API docs: http://localhost:8080/swagger-ui.html · spec: `web/openapi.json`.

## API

| Method | Path | Who |
|---|---|---|
| POST | `/api/v1/requests` | any user (201 + `Location`) |
| GET | `/api/v1/requests/{id}` | owner or treasurer (others get 404) |
| GET | `/api/v1/requests?status=&mine=&order=&page=&size=` | members see only their own |
| POST | `/api/v1/requests/{id}/approve` · `/reject` · `/pay` | treasurer |
| GET | `/api/v1/me` | any user |

Errors are RFC 9457 Problem Details: `400` validation, `401`/`403` auth, `404`, `409` illegal transition or
version conflict. Bank account numbers are masked in responses unless the viewer is the treasurer.

## Tests

```sh
./gradlew build             # unit, @WebMvcTest slices, Testcontainers integration (needs Docker)
cd web && npm run lint && npm run build
```

- Domain tests have no Spring context; every status × command pair is in a parameterised transition table.
- `RequestFlowIntegrationTest` runs against real Postgres: submit → approve → pay, role/row access, DB CHECK
  constraints, an optimistic-locking race, and a check that `web/openapi.json` matches the running API.

### Changing the API
The front end's types are generated from the OpenAPI spec so the contract can't drift:

```sh
UPDATE_OPENAPI=true ./gradlew :core-service:test --tests '*RequestFlowIntegrationTest'   # rewrite web/openapi.json
cd web && npm run gen:api                                                                  # regenerate src/api/schema.d.ts
```

## Layout

```
core-service/   Spring Boot app (requests, identity, shared; domain / application / adapters per module)
web/            React + TypeScript (Vite)
docs/adr/       Architecture decision records
```

Not yet built (later phases): receipt uploads and audit trail (P1), segregation of duties (P1), Keycloak (P1).
