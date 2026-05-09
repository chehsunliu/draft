# AGENTS.md / CLAUDE.md

## Things Must Do When Committing to GitHub

1. Never commit directly to the `main` branch. Always create a `pr/` branch first.
2. Create PRs using a branch name that starts with `pr/` and is at most 20 characters long.
3. Never force push.
4. Auto-merge may be enabled when the PR changes are limited to UI and/or documentation.

## Things Must Do After Code Change

### `itx-rs`

1. `cargo fmt`
2. `cargo test`
3. `cargo build`

### `itx-go`

1. `go fmt ./...`
2. `go test ./...`
3. `go build ./...`

### `./integration-tests/pytest`:

1. Add tests to `./integration-tests/pytest/tests/` when adding features or fixing bugs.
2. `make fmt`
3. `make check`
4. If any SQL files (e.g. migrations, seed data) were changed, restart docker compose (`docker compose down && docker compose up -d`) before running tests so the database picks up the new schema/data.
5. Run the related test file first with `uv run pytest ./path/to/test/file` to iterate quickly, then run `make it` for the full suite once it passes.
