# API contract snapshot (F-05)

`openapi-v1.json` is the frozen PlanWise API contract (OpenAPI 3.1) for MVP v1. Server base: `/api`; all
operations are under `/v1`. The frontend generates its API types from this file (FE-02).

- **Owner:** TV1. Only TV1 updates this file.
- **Freeze:** the contract is frozen from the moment the PR that adds the verified snapshot merges into
  `develop`.
- **Changing the contract:** follow `docs/plan/01_CONTRACT_GATES.md` (TV1 approval first). The endpoint set is
  guarded in CI by `OpenApiContractTests`, so adding or removing an endpoint fails `./gradlew build` until the
  approved change also updates that test and this snapshot.

## Regenerate

From `Code/backend/` (H2 test profile; no Docker, database or `.env` needed):

```bash
./gradlew exportOpenApi
```

The output has sorted keys and is byte-stable: two runs on the same code produce the same SHA-256. Normal
`./gradlew test` / `build` never rewrite this file.
