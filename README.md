# draft

A demonstration project for performing integration testing on multi-module backend systems using **Vitest** and **Pytest** as test runners.

The project is designed to support multiple backend languages behind a shared API spec. For now, the implementations are **Go (Gin)** and **Rust (Axum)**; more may be added later.

## Goals

- Show how to drive integration tests for backend services from a language-agnostic test runner (Vitest / Pytest).
- Provide functionally identical implementations across languages (currently Go and Rust) behind the same API spec.
- Demonstrate swappable infrastructure (databases, message queues, caches) behind interfaces, so the backend and worker code does not depend on a concrete implementation.
- Run the full matrix in GitHub Actions with real services spun up as containers — not mocks.

## Architecture

Each language has its own workspace (`draft-go/`, `draft-rs/`, ...), and each workspace is split into two modules:

- **backend** — HTTP API server. Receives client requests, reads/writes to the database and cache, publishes messages to a queue.
- **worker** — Consumes messages from the queue, performs background work, reads/writes to the database and cache.

A typical request flow:

```
client -> backend -> (db / cache) -> queue -> worker -> (db / cache)
```

Both backend and worker depend on **interfaces** for their infrastructure dependencies. Concrete implementations (e.g. MariaDB vs. Postgres, RabbitMQ vs. SQS) are wired in at startup and are invisible to business logic.

## Layout

```
.
├── draft-go/             # Go workspace
│   ├── backend/          # gin HTTP server
│   ├── worker/           # queue consumer
│   └── core/             # shared interfaces, domain types
├── draft-rs/             # Rust workspace
│   ├── backend/          # axum HTTP server
│   ├── worker/           # queue consumer
│   └── core/             # shared interfaces, domain types
├── tests/
│   ├── vitest/           # TypeScript integration suite
│   └── pytest/           # Python integration suite
└── .github/workflows/
```

Additional language workspaces (e.g. `draft-py/`, `draft-ts/`) can be added later following the same convention.

## Infrastructure choices

| Concern   | Options           |
|-----------|-------------------|
| Database  | MariaDB, Postgres |
| Queue     | RabbitMQ, SQS     |
| Cache     | Redis             |

Each implementation lives behind the same interface, so any combination is valid.

## API spec

All backend implementations expose **the same HTTP API** — same routes, same request/response shapes, same status codes. The integration test suites are written once per runner and run against any implementation.

## CI matrix

GitHub Actions runs integration tests across selected combinations of:

- test runner: `vitest` | `pytest`
- backend language: `go` | `rust` (more in the future)
- database: `mariadb` | `postgres`
- queue: `rabbitmq` | `sqs`
- cache: `redis`

Example flows:

1. `vitest + mariadb + rabbitmq + redis + rust`
2. `pytest + postgres + sqs + redis + go`
3. `vitest + postgres + rabbitmq + redis + rust`

Not every combination is exercised — the matrix picks a representative subset.

## Status

Early scaffolding. Nothing implemented yet.
