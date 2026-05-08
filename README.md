# ITX

[![ITX Testing](https://github.com/chehsunliu/itx/actions/workflows/itx-testing.yml/badge.svg)](https://github.com/chehsunliu/itx/actions/workflows/itx-testing.yml)

A demonstration project for performing integration testing on multi-module backend systems using **Pytest** as the test runner.

The project is designed to support multiple backend languages behind a shared API spec. For now, the implementations are **Go (Gin)**, **Rust (Axum)**, and **Kotlin (Ktor)**; more may be added later.

## Goals

- Show how to drive integration tests for backend services from a language-agnostic test runner (Pytest).
- Provide functionally identical implementations across languages (currently Go and Rust) behind the same API spec.
- Demonstrate swappable infrastructure (databases, message queues, caches) behind interfaces, so the backend and worker code does not depend on a concrete implementation.
- Run the full matrix in GitHub Actions with real services spun up as containers — not mocks.

## Development

The pytest integration suite picks which backend to build and run via the `ITX_LANG` env var (`rust` by default, or `golang` or `kotlin`). The fixtures handle building the binary and starting/stopping the server — you don't need to run anything in `itx-rs/`, `itx-go/`, or `itx-kt/` yourself.

Prerequisites:

- Python 3.14 + [`uv`](https://docs.astral.sh/uv/)
- Rust toolchain (stable) — for `ITX_LANG=rust`
- Go (version in `itx-go/itx-backend/go.mod`) — for `ITX_LANG=golang`
- JDK 25 + GNU make — for `ITX_LANG=kotlin` (the Gradle wrapper handles the rest)

One-time setup:

```sh
cd integration-tests
uv sync
```

Run the suite against the rust backend (default):

```sh
make test
# or, explicitly:
ITX_LANG=rust make test
```

Run the same suite against the go backend:

```sh
ITX_LANG=golang make test
```

Run the same suite against the kotlin backend:

```sh
ITX_LANG=kotlin make test
```

### Switching the infra profile

Which infrastructure the suite runs against is controlled by `ITX_TEST_PROFILE` (default `aws`):

```sh
ITX_TEST_PROFILE=aws make test     # postgres
ITX_TEST_PROFILE=onprem make test  # mariadb
```

A profile bundles a set of infra choices (database, and later queue/cache/etc.) into a single switch. The profile names — `aws`, `onprem` — are just labels for the bundles used in this demo; they don't constrain what you can run where (you can absolutely deploy mariadb on AWS in real life). They're packaged this way because flipping every piece (`ITX_DB_PROVIDER`, `ITX_*_HOST`, …) one by one would be tedious.

### Running multiple checkouts in parallel

The `integration-tests/docker-compose.yml` deliberately publishes every service on a **random host port** (`127.0.0.1:0:<container-port>`) instead of a fixed one. This is intentional: it lets you bring up multiple independent stacks side by side — for example, one per `git worktree`, or a second `git clone` for another Claude Code session — without host-port collisions.

To keep the stacks fully isolated (separate containers, networks, and volumes), set `COMPOSE_PROJECT_NAME` to a unique value per checkout:

```sh
# in checkout A
COMPOSE_PROJECT_NAME=itx-a docker compose -f integration-tests/docker-compose.yml up -d

# in checkout B (different worktree / clone)
COMPOSE_PROJECT_NAME=itx-b docker compose -f integration-tests/docker-compose.yml up -d
```

The pytest fixtures discover the dynamic ports at runtime, so each session talks to its own stack as long as `COMPOSE_PROJECT_NAME` is exported in that shell.
